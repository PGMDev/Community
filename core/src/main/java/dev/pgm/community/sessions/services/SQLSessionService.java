package dev.pgm.community.sessions.services;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import dev.pgm.community.Community;
import dev.pgm.community.database.DatabaseExecutor;
import dev.pgm.community.database.Query;
import dev.pgm.community.feature.SQLFeatureBase;
import dev.pgm.community.sessions.Session;
import dev.pgm.community.sessions.SessionQuery;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class SQLSessionService extends SQLFeatureBase<Session, SessionQuery>
    implements SessionDataQuery {

  // Other servers write sessions to the same tables, so cached entries must expire
  private static final long CACHE_EXPIRY_SECONDS = 60;
  private static final long CACHE_MAX_SIZE = 5000;

  private final LoadingCache<SessionQuery, SessionData> sessionCache;
  private final String upsertLatestSessionQuery;
  private final CompletableFuture<Integer> latestTableReady;
  private final CompletableFuture<Void> cleanupIndexesReady;
  private CompletableFuture<Void> startupReady = CompletableFuture.completedFuture(null);

  // Writes for a player are chained so they reach the database in the order they were issued
  private final Map<UUID, CompletableFuture<Void>> pendingWrites;

  public SQLSessionService() {
    super(TABLE_NAME, TABLE_FIELDS);
    this.sessionCache = CacheBuilder.newBuilder()
        .maximumSize(CACHE_MAX_SIZE)
        .expireAfterWrite(CACHE_EXPIRY_SECONDS, TimeUnit.SECONDS)
        .build(CacheLoader.from(key -> new SessionData()));
    this.upsertLatestSessionQuery = DatabaseExecutor.getDialect().upsertLatestSessionQuery();
    this.pendingWrites = new ConcurrentHashMap<>();

    getTableReady()
        .thenRun(() -> DatabaseExecutor.createIndexAsync(TABLE_NAME, "idx_player", "player"));
    this.latestTableReady = DatabaseExecutor.executeUpdateAsync(
        Query.createTable(LATEST_TABLE_NAME, LATEST_TABLE_FIELDS));
    latestTableReady.thenRun(
        () -> DatabaseExecutor.createIndexAsync(LATEST_TABLE_NAME, "idx_session_id", "session_id"));
    this.cleanupIndexesReady = CompletableFuture.allOf(
        getTableReady()
            .thenCompose(ignored -> DatabaseExecutor.createIndexAsync(
                TABLE_NAME, "idx_server_end_time", "server, end_time")),
        latestTableReady.thenCompose(ignored -> DatabaseExecutor.createIndexAsync(
            LATEST_TABLE_NAME, "idx_server_end_time", "server, end_time")));
  }

  @Override
  public void save(Session session) {
    sessionCache.getUnchecked(new SessionQuery(session.getPlayerId(), false)).setSession(session);

    SessionQuery ignoreDisguisedKey = new SessionQuery(session.getPlayerId(), true);
    if (session.isDisguised()) {
      // Latest non-disguised session is unchanged, but the cached copy may be stale
      sessionCache.invalidate(ignoreDisguisedKey);
    } else {
      sessionCache.getUnchecked(ignoreDisguisedKey).setSession(session);
    }

    enqueueWrite(
        session.getPlayerId(),
        () -> DatabaseExecutor.executeUpdateAsync(
                INSERT_SESSION_QUERY,
                session.getSessionId().toString(),
                session.getPlayerId().toString(),
                session.isDisguised(),
                session.getServerName(),
                session.getStartDate().toEpochMilli(),
                toEpochMilli(session.getEndDate()))
            .thenCompose(ignored -> upsertLatest(session, false))
            .thenCompose(ignored -> session.isDisguised()
                ? CompletableFuture.completedFuture(0)
                : upsertLatest(session, true)));
  }

  public void updateSessionEndTime(Session session) {
    endSessionWrite(session);
  }

  public void updateSessionEndTimeSync(Session session) {
    try {
      endSessionWrite(session).join();
    } catch (Exception exception) {
      Community.get()
          .getLogger()
          .warning(
              "Failed to end session " + session.getSessionId() + ": " + exception.getMessage());
    }
  }

  public CompletableFuture<Void> closeOrphanedSessions(String server, Instant endTime) {
    long end = endTime.toEpochMilli();
    CompletableFuture<Void> cleanup =
        cleanupIndexesReady.thenCompose(ignored -> CompletableFuture.allOf(
            DatabaseExecutor.executeUpdateAsync(CLOSE_ORPHANED_SESSIONS_QUERY, end, server),
            DatabaseExecutor.executeUpdateAsync(CLOSE_ORPHANED_LATEST_QUERY, end, server)));
    startupReady = cleanup.handle((ignored, error) -> null);
    return cleanup;
  }

  @Override
  public CompletableFuture<List<Session>> queryList(SessionQuery target) {
    return CompletableFuture.completedFuture(List.of());
  }

  @Override
  public CompletableFuture<Session> query(SessionQuery target) {
    SessionData data = sessionCache.getUnchecked(target);

    if (data.isLoaded()) {
      return CompletableFuture.completedFuture(data.getSession());
    }

    // Wait for in-flight writes so the database row reflects this server's latest state
    CompletableFuture<Void> pending =
        pendingWrites.getOrDefault(target.playerId(), CompletableFuture.completedFuture(null));
    return startupReady
        .thenCompose(ignored -> pending)
        .thenCompose(ignored -> DatabaseExecutor.queryFirstAsync(
            SELECT_LATEST_SESSION_QUERY,
            result -> {
              String id = result.getString("session_id");
              String player = result.getString("player");
              boolean disguised = result.getBoolean("disguised");
              String server = result.getString("server");

              long startTime = result.getLong("start_time");
              Instant start = result.wasNull() ? null : Instant.ofEpochMilli(startTime);

              long endTime = result.getLong("end_time");
              Instant end = result.wasNull() ? null : Instant.ofEpochMilli(endTime);

              return new Session(
                  UUID.fromString(id), UUID.fromString(player), disguised, server, start, end);
            },
            target.playerId().toString(),
            target.ignoreDisguised()))
        .thenApplyAsync(data::loadIfAbsent);
  }

  private CompletableFuture<Void> endSessionWrite(Session session) {
    // Cached entries may hold a copy loaded from the database rather than this instance
    for (boolean ignoreDisguised : new boolean[] {false, true}) {
      SessionData data =
          sessionCache.getIfPresent(new SessionQuery(session.getPlayerId(), ignoreDisguised));
      if (data != null) data.updateEndDate(session);
    }

    Long endTime = toEpochMilli(session.getEndDate());
    String sessionId = session.getSessionId().toString();
    return enqueueWrite(
        session.getPlayerId(),
        () -> DatabaseExecutor.executeUpdateAsync(UPDATE_SESSION_ENDTIME_QUERY, endTime, sessionId)
            .thenCompose(ignored -> DatabaseExecutor.executeUpdateAsync(
                UPDATE_LATEST_ENDTIME_QUERY, endTime, sessionId)));
  }

  private CompletableFuture<Integer> upsertLatest(Session session, boolean ignoreDisguised) {
    return DatabaseExecutor.executeUpdateAsync(
        upsertLatestSessionQuery,
        session.getPlayerId().toString(),
        ignoreDisguised,
        session.getSessionId().toString(),
        session.isDisguised(),
        session.getServerName(),
        session.getStartDate().toEpochMilli(),
        toEpochMilli(session.getEndDate()));
  }

  private CompletableFuture<Void> enqueueWrite(
      UUID playerId, Supplier<CompletableFuture<?>> write) {
    CompletableFuture<Void> queued = pendingWrites.compute(playerId, (id, previous) -> {
      CompletableFuture<Void> base = previous == null ? startupReady : previous;
      return base.thenCompose(ignored -> write.get()).handle((result, error) -> {
        if (error != null) {
          Community.get()
              .getLogger()
              .warning("Failed to write session for " + playerId + ": " + error.getMessage());
        }
        return null;
      });
    });
    queued.whenComplete((ignored, error) -> pendingWrites.remove(playerId, queued));
    return queued;
  }

  private static Long toEpochMilli(Instant instant) {
    return instant == null ? null : instant.toEpochMilli();
  }

  private static class SessionData {

    private Session session;
    private boolean loaded;

    // A session that ended while the database lookup was in flight
    private Session pendingEnd;

    public synchronized Session getSession() {
      return session;
    }

    public synchronized boolean isLoaded() {
      return loaded;
    }

    public synchronized void setSession(Session session) {
      this.loaded = true;
      this.session = session;
      this.pendingEnd = null;
    }

    /**
     * Stores a session loaded from the database, unless a newer one was set in the meantime. Any
     * end time recorded during the lookup is applied, since the row may have been read before it
     * was written.
     */
    public synchronized Session loadIfAbsent(Session session) {
      if (!loaded) {
        if (pendingEnd != null
            && session != null
            && session.getSessionId().equals(pendingEnd.getSessionId())) {
          session.setEndDate(pendingEnd.getEndDate());
        }
        setSession(session);
      }
      return this.session;
    }

    public synchronized void updateEndDate(Session ended) {
      if (!loaded) {
        pendingEnd = ended;
      } else if (session != null
          && session != ended
          && session.getSessionId().equals(ended.getSessionId())) {
        session.setEndDate(ended.getEndDate());
      }
    }
  }
}

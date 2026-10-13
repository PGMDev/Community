package dev.pgm.community.sessions.services;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import dev.pgm.community.Community;
import dev.pgm.community.database.DatabaseExecutor;
import dev.pgm.community.database.Query;
import dev.pgm.community.feature.SQLFeatureBase;
import dev.pgm.community.sessions.Session;
import dev.pgm.community.sessions.SessionQuery;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class SQLSessionService extends SQLFeatureBase<Session, SessionQuery>
    implements SessionDataQuery {

  private static final long END_BATCH_DELAY_MILLIS = 250;
  private static final int END_BATCH_SIZE = 250;
  private static final long CACHE_EXPIRY_SECONDS = 60;
  private static final long CACHE_MAX_SIZE = 5000;

  private final Cache<SessionQuery, Session> sessionCache;
  private final String upsertLatestSessionQuery;
  private final CompletableFuture<Void> orphanIndexesReady;
  private final CompletableFuture<Void> tablesReady;
  private volatile CompletableFuture<Void> readsReady = CompletableFuture.completedFuture(null);
  private final Map<UUID, CompletableFuture<Void>> pendingWrites;
  private final Object endBatchLock = new Object();
  private List<PendingEnd> endBatch = new ArrayList<>();
  private CompletableFuture<Void> endBatchWritten = new CompletableFuture<>();
  private boolean flushed;

  private record PendingEnd(String sessionId, long endTime) {}

  public SQLSessionService() {
    super(TABLE_NAME, TABLE_FIELDS);
    this.sessionCache = CacheBuilder.newBuilder()
        .maximumSize(CACHE_MAX_SIZE)
        .expireAfterWrite(CACHE_EXPIRY_SECONDS, TimeUnit.SECONDS)
        .build();
    this.upsertLatestSessionQuery = DatabaseExecutor.getDialect().upsertLatestSessionQuery();
    this.pendingWrites = new ConcurrentHashMap<>();

    CompletableFuture<Integer> latestTableReady = DatabaseExecutor.executeUpdateAsync(
        Query.createTable(LATEST_TABLE_NAME, LATEST_TABLE_FIELDS));
    latestTableReady.thenRun(
        () -> DatabaseExecutor.createIndexAsync(LATEST_TABLE_NAME, "idx_session_id", "session_id"));
    this.tablesReady =
        CompletableFuture.allOf(getTableReady(), latestTableReady).handle((ignored, _) -> null);

    this.orphanIndexesReady = CompletableFuture.allOf(
        getTableReady()
            .thenCompose(ignored -> DatabaseExecutor.createIndexAsync(
                TABLE_NAME, "idx_server_end_time", "server, end_time")),
        latestTableReady.thenCompose(ignored -> DatabaseExecutor.createIndexAsync(
            LATEST_TABLE_NAME, "idx_server_end_time", "server, end_time")));
  }

  @Override
  public void save(Session session) {
    sessionCache.put(new SessionQuery(session.getPlayerId(), false), session);
    SessionQuery publicKey = new SessionQuery(session.getPlayerId(), true);
    if (session.isDisguised()) {
      sessionCache.invalidate(publicKey);
    } else {
      sessionCache.put(publicKey, session);
    }

    enqueueWrite(
        session.getPlayerId(),
        () -> CompletableFuture.allOf(
            DatabaseExecutor.executeUpdateAsync(
                INSERT_SESSION_QUERY,
                session.getSessionId().toString(),
                session.getPlayerId().toString(),
                session.isDisguised(),
                session.getServerName(),
                session.getStartDate().toEpochMilli(),
                toEpochMilli(session.getEndDate())),
            upsertLatestRows(session)
                .exceptionallyCompose(_ -> upsertLatestRows(session))
                .exceptionally(error -> {
                  Community.get()
                      .getLogger()
                      .warning("Failed to update the latest session of " + session.getPlayerId()
                          + ", so lookups may show their previous one: " + error.getMessage());
                  return null;
                })));
  }

  private CompletableFuture<Void> upsertLatestRows(Session session) {
    return upsertLatest(session, false)
        .thenCompose(ignored -> session.isDisguised()
            ? CompletableFuture.completedFuture(0)
            : upsertLatest(session, true))
        .thenApply(ignored -> null);
  }

  public CompletableFuture<Integer> closeOrphanedSessions(String server, Instant bootTime) {
    Object[] params = {server, bootTime.toEpochMilli()};
    CompletableFuture<Integer> cleanup = orphanIndexesReady.thenCompose(
        ignored -> DatabaseExecutor.executeUpdateOrFailAsync(CLOSE_ORPHANED_SESSIONS_QUERY, params)
            .thenCombine(
                DatabaseExecutor.executeUpdateOrFailAsync(CLOSE_ORPHANED_LATEST_QUERY, params),
                (closed, ignoredLatest) -> closed));
    readsReady = cleanup.handle((ignored, _) -> null);
    return cleanup;
  }

  @Override
  public CompletableFuture<List<Session>> queryList(SessionQuery target) {
    return CompletableFuture.completedFuture(List.of());
  }

  @Override
  public CompletableFuture<Session> query(SessionQuery target) {
    Session cached = sessionCache.getIfPresent(target);
    if (cached != null) return CompletableFuture.completedFuture(cached);

    CompletableFuture<Void> pending =
        pendingWrites.getOrDefault(target.playerId(), CompletableFuture.completedFuture(null));
    if (!pending.isDone()) writeCurrentBatch();
    return readsReady
        .thenCompose(ignored -> pending)
        .thenCompose(ignored -> DatabaseExecutor.queryFirstAsync(
            SELECT_LATEST_SESSION_QUERY,
            SQLSessionService::readSession,
            target.playerId().toString(),
            target.ignoreDisguised()))
        .thenApply(session -> {
          Session current = session != null && !pendingWrites.containsKey(target.playerId())
              ? sessionCache.asMap().putIfAbsent(target, session)
              : sessionCache.getIfPresent(target);
          return current != null ? current : session;
        });
  }

  public void updateSessionEndTime(UUID playerId, UUID sessionId, Instant endTime) {
    PendingEnd end = new PendingEnd(sessionId.toString(), endTime.toEpochMilli());
    enqueueWrite(playerId, () -> queueEnd(end));
  }

  public CompletableFuture<Void> shutdownFlush() {
    synchronized (endBatchLock) {
      flushed = true;
    }
    writeCurrentBatch();

    return CompletableFuture.allOf(pendingWrites.values().toArray(CompletableFuture[]::new));
  }

  private void writeCurrentBatch() {
    CompletableFuture<Void> batch;
    synchronized (endBatchLock) {
      batch = endBatchWritten;
    }
    writeEndBatch(batch);
  }

  private CompletableFuture<Void> queueEnd(PendingEnd end) {
    CompletableFuture<Void> written;
    boolean writeNow;
    synchronized (endBatchLock) {
      written = endBatchWritten;
      endBatch.add(end);
      writeNow = flushed || endBatch.size() >= END_BATCH_SIZE;
      if (endBatch.size() == 1 && !writeNow) {
        CompletableFuture.delayedExecutor(END_BATCH_DELAY_MILLIS, TimeUnit.MILLISECONDS)
            .execute(() -> writeEndBatch(written));
      }
    }
    if (writeNow) writeEndBatch(written);
    return written;
  }

  private void writeEndBatch(CompletableFuture<Void> written) {
    List<PendingEnd> batch;
    synchronized (endBatchLock) {
      if (endBatchWritten != written || endBatch.isEmpty()) return;
      batch = endBatch;
      endBatch = new ArrayList<>();
      endBatchWritten = new CompletableFuture<>();
    }

    Object[] params = new Object[batch.size() * 3];
    int i = 0;
    for (PendingEnd end : batch) {
      params[i++] = end.sessionId();
      params[i++] = end.endTime();
    }
    for (PendingEnd end : batch) params[i++] = end.sessionId();

    writeEnds(batch.size(), params)
        .exceptionallyCompose(_ -> writeEnds(batch.size(), params))
        .whenComplete((ignored, error) -> {
          if (error != null) {
            Community.get()
                .getLogger()
                .warning("Failed to end " + batch.size() + " sessions. They will read as online on"
                    + " this server until next startup, then be closed as zero-length: "
                    + error.getMessage());
          }
          written.complete(null);
        });
  }

  private static CompletableFuture<Void> writeEnds(int count, Object[] params) {
    try {
      return CompletableFuture.allOf(
          DatabaseExecutor.executeUpdateOrFailAsync(endQuery(TABLE_NAME, "id", count), params),
          DatabaseExecutor.executeUpdateOrFailAsync(
              endQuery(LATEST_TABLE_NAME, "session_id", count), params));
    } catch (RuntimeException e) {
      return CompletableFuture.failedFuture(e);
    }
  }

  private static String endQuery(String table, String idColumn, int count) {
    return "UPDATE " + table + " SET end_time = CASE " + idColumn
        + " WHEN ? THEN ?".repeat(count) + " END WHERE " + idColumn + " IN "
        + placeholders(count);
  }

  private CompletableFuture<Integer> upsertLatest(Session session, boolean ignoreDisguised) {
    return DatabaseExecutor.executeUpdateOrFailAsync(
        upsertLatestSessionQuery,
        session.getPlayerId().toString(),
        ignoreDisguised,
        session.getSessionId().toString(),
        session.isDisguised(),
        session.getServerName(),
        session.getStartDate().toEpochMilli(),
        toEpochMilli(session.getEndDate()));
  }

  private void enqueueWrite(UUID playerId, Supplier<CompletableFuture<?>> write) {
    CompletableFuture<Void> queued = new CompletableFuture<>();
    CompletableFuture<Void> previous = pendingWrites.put(playerId, queued);
    CompletableFuture<Void> base = previous == null ? tablesReady : previous;
    base.thenCompose(ignored -> write.get()).whenComplete((ignored, error) -> {
      if (error != null) {
        Community.get()
            .getLogger()
            .warning("Failed to write session for " + playerId + ": " + error.getMessage());
      }
      pendingWrites.remove(playerId, queued);
      queued.complete(null);
    });
  }

  private static Session readSession(ResultSet result) throws SQLException {
    String id = result.getString("session_id");
    String player = result.getString("player");
    boolean disguised = result.getBoolean("disguised");
    String server = result.getString("server");

    long startTime = result.getLong("start_time");
    Instant start = result.wasNull() ? null : Instant.ofEpochMilli(startTime);

    long endTime = result.getLong("end_time");
    Instant end = result.wasNull() ? null : Instant.ofEpochMilli(endTime);

    return new Session(UUID.fromString(id), UUID.fromString(player), disguised, server, start, end);
  }

  private static String placeholders(int count) {
    return "(" + String.join(", ", Collections.nCopies(count, "?")) + ")";
  }

  private static Long toEpochMilli(Instant instant) {
    return instant == null ? null : instant.toEpochMilli();
  }
}

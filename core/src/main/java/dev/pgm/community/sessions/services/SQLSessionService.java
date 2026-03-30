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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SQLSessionService extends SQLFeatureBase<Session, SessionQuery>
    implements SessionDataQuery {

  private final LoadingCache<SessionQuery, SessionData> sessionCache;
  private final String upsertLatestSessionQuery;

  public SQLSessionService() {
    super(TABLE_NAME, TABLE_FIELDS);
    this.sessionCache = CacheBuilder.newBuilder()
        .build(CacheLoader.from(key -> new SessionData(key.playerId(), key.ignoreDisguised())));
    this.upsertLatestSessionQuery = DatabaseExecutor.getDialect().upsertLatestSessionQuery();
  }

  @Override
  public void createTable() {
    super.createTable();
    DatabaseExecutor.executeUpdateAsync(Query.createTable(LATEST_TABLE_NAME, LATEST_TABLE_FIELDS));
  }

  @Override
  public void save(Session session) {
    SessionData query = sessionCache.getUnchecked(new SessionQuery(session.getPlayerId(), false));
    query.setSession(session);

    query = sessionCache.getUnchecked(new SessionQuery(session.getPlayerId(), true));
    query.invalidate();

    DatabaseExecutor.executeUpdateAsync(
        INSERT_SESSION_QUERY,
        session.getSessionId().toString(),
        session.getPlayerId().toString(),
        session.isDisguised(),
        session.getServerName(),
        session.getStartDate().toEpochMilli(),
        session.getEndDate() == null ? null : session.getEndDate().toEpochMilli());

    DatabaseExecutor.executeUpdateAsync(
        upsertLatestSessionQuery,
        session.getPlayerId().toString(),
        false,
        session.getSessionId().toString(),
        session.isDisguised(),
        session.getServerName(),
        session.getStartDate().toEpochMilli(),
        session.getEndDate() == null ? null : session.getEndDate().toEpochMilli());

    if (!session.isDisguised()) {
      DatabaseExecutor.executeUpdateAsync(
          upsertLatestSessionQuery,
          session.getPlayerId().toString(),
          true,
          session.getSessionId().toString(),
          session.isDisguised(),
          session.getServerName(),
          session.getStartDate().toEpochMilli(),
          session.getEndDate() == null ? null : session.getEndDate().toEpochMilli());
    }
  }

  public void updateSessionEndTime(Session session) {
    DatabaseExecutor.executeUpdateAsync(
        UPDATE_SESSION_ENDTIME_QUERY,
        session.getEndDate() == null ? null : session.getEndDate().toEpochMilli(),
        session.getSessionId().toString());
    DatabaseExecutor.executeUpdateAsync(
        UPDATE_LATEST_ENDTIME_QUERY,
        session.getEndDate() == null ? null : session.getEndDate().toEpochMilli(),
        session.getSessionId().toString());
  }

  public void updateSessionEndTimeSync(Session session) {
    Long endTime = session.getEndDate() == null ? null : session.getEndDate().toEpochMilli();
    try {
      DatabaseExecutor.executeUpdateAsync(
              UPDATE_SESSION_ENDTIME_QUERY, endTime, session.getSessionId().toString())
          .join();
      DatabaseExecutor.executeUpdateAsync(
              UPDATE_LATEST_ENDTIME_QUERY, endTime, session.getSessionId().toString())
          .join();
    } catch (Exception exception) {
      Community.get()
          .getLogger()
          .warning(
              "Failed to end session " + session.getSessionId() + ": " + exception.getMessage());
    }
  }

  @Override
  public CompletableFuture<List<Session>> queryList(SessionQuery target) {
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public CompletableFuture<Session> query(SessionQuery target) {
    SessionData data = sessionCache.getUnchecked(target);

    if (data.isLoaded()) {
      return CompletableFuture.completedFuture(data.getSession());
    } else {
      return DatabaseExecutor.queryFirstAsync(
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
              target.ignoreDisguised())
          .thenApplyAsync(result -> {
            if (result != null) {
              data.setSession(result);
            }
            return data.getSession();
          });
    }
  }

  private static class SessionData {

    private final UUID playerId;
    private final boolean ignoreDisguised;
    private Session session;
    private boolean loaded;

    public SessionData(UUID playerId, boolean ignoreDisguised) {
      this.playerId = playerId;
      this.ignoreDisguised = ignoreDisguised;
      this.session = null;
      this.loaded = false;
    }

    public UUID getPlayerId() {
      return playerId;
    }

    public boolean isDisguiseIgnored() {
      return ignoreDisguised;
    }

    public Session getSession() {
      return session;
    }

    public void setSession(Session session) {
      loaded = true;
      this.session = session;
    }

    public boolean isLoaded() {
      return loaded;
    }

    public void invalidate() {
      loaded = false;
      session = null;
    }
  }
}

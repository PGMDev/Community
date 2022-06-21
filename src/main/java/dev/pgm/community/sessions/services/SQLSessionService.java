package dev.pgm.community.sessions.services;

import co.aikar.idb.DB;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import dev.pgm.community.Community;
import dev.pgm.community.feature.SQLFeatureBase;
import dev.pgm.community.sessions.Session;
import dev.pgm.community.sessions.SessionQuery;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;

public class SQLSessionService extends SQLFeatureBase<Session, SessionQuery>
    implements SessionDataQuery {

  private final LoadingCache<SessionQuery, SessionData> sessionCache;

  public SQLSessionService() {
    super(TABLE_NAME, TABLE_FIELDS);
    this.sessionCache =
        CacheBuilder.newBuilder()
            .build(
                new CacheLoader<SessionQuery, SessionData>() {
                  @Override
                  public SessionData load(@Nonnull SessionQuery key) {
                    return new SessionData(key.getPlayerId(), key.ignoreDisguised());
                  }
                });
  }

  @Override
  public void save(Session session) {
    SessionData query = sessionCache.getUnchecked(new SessionQuery(session.getPlayerId(), false));
    query.setSession(session);

    query = sessionCache.getUnchecked(new SessionQuery(session.getPlayerId(), true));
    query.invalidate();

    DB.executeUpdateAsync(
        INSERT_SESSION_QUERY,
        session.getSessionId().toString(),
        session.getPlayerId().toString(),
        session.isDisguised(),
        session.getServerName(),
        session.getStartDate().toEpochMilli(),
        session.getEndDate() == null ? null : session.getEndDate().toEpochMilli());
  }

  public void updateSessionEndTime(Session session) {
    DB.executeUpdateAsync(
        UPDATE_SESSION_ENDTIME_QUERY,
        session.getEndDate() == null ? null : session.getEndDate().toEpochMilli(),
        session.getSessionId().toString());
  }

  public void endOngoingSessions() {
    DB.executeUpdateAsync(
        UPDATE_ONGOING_SESSION_ENDING_QUERY,
        Instant.now().toEpochMilli(),
        Community.get().getServerId());
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
      return DB.getFirstRowAsync(
              target.ignoreDisguised() ? SELECT_DISGUISED_SESSION_QUERY : SELECT_SESSION_QUERY,
              target.getPlayerId().toString())
          .thenApplyAsync(
              result -> {
                if (result != null) {
                  String id = result.getString("id");

                  String player = result.getString("player");
                  boolean disguised = result.get("disguised");

                  String server = result.getString("server");

                  long startTime = Long.parseLong(result.getString("start_time"));
                  Object endTime = result.get("end_time");

                  data.setSession(
                      new Session(
                          UUID.fromString(id),
                          UUID.fromString(player),
                          disguised,
                          server,
                          Instant.ofEpochMilli(startTime),
                          endTime == null ? null : Instant.ofEpochMilli((Long) endTime)));
                }
                return data.getSession();
              });
    }
  }

  private class SessionData {

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

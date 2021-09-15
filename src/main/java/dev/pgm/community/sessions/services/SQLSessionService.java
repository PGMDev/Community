package dev.pgm.community.sessions.services;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import dev.pgm.community.database.DatabaseConnection;
import dev.pgm.community.feature.SQLFeatureBase;
import dev.pgm.community.sessions.Session;
import dev.pgm.community.sessions.SessionQuery;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;
import tc.oc.pgm.util.concurrent.ThreadSafeConnection.Query;

public class SQLSessionService extends SQLFeatureBase<Session, SessionQuery> {

  private static final String TABLE_NAME = "sessions";
  private static final String TABLE_FIELDS =
      "(id VARCHAR(36) PRIMARY KEY, player VARCHAR(36), disguised BOOL, server VARCHAR(32), start_time BIGINT, end_time BIGINT)";

  private final LoadingCache<SessionQuery, SelectLatestSessionQuery> sessionCache;

  public SQLSessionService(DatabaseConnection database) {
    super(database, TABLE_NAME, TABLE_FIELDS);
    this.sessionCache =
        CacheBuilder.newBuilder()
            .build(
                new CacheLoader<SessionQuery, SelectLatestSessionQuery>() {
                  @Override
                  public SelectLatestSessionQuery load(@Nonnull SessionQuery key) {
                    return new SelectLatestSessionQuery(key.getPlayerId(), key.ignoreDisguised());
                  }
                });
  }

  @Override
  public void save(Session session) {
    SelectLatestSessionQuery query =
        sessionCache.getUnchecked(new SessionQuery(session.getPlayerId(), false));
    query.setSession(session);

    query = sessionCache.getUnchecked(new SessionQuery(session.getPlayerId(), true));
    query.invalidate();

    getDatabase().submitQuery(new InsertQuery(session));
  }

  public void updateSessionEndTime(Session session) {
    getDatabase().submitQuery(new UpdateSessionEndTimeQuery(session));
  }

  @Override
  public CompletableFuture<List<Session>> queryList(SessionQuery target) {
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public CompletableFuture<Session> query(SessionQuery target) {
    SelectLatestSessionQuery query = sessionCache.getUnchecked(target);

    if (query.hasFetched()) {
      return CompletableFuture.completedFuture(query.getSession());
    } else {
      return getDatabase()
          .submitQueryComplete(query)
          .thenApplyAsync(q -> SelectLatestSessionQuery.class.cast(q).getSession());
    }
  }

  private class InsertQuery implements Query {

    private static final String INSERT_SESSION_QUERY =
        "INSERT INTO "
            + TABLE_NAME
            + "(id, player, disguised, server, start_time, end_time) VALUES (?, ?, ?, ?, ?, ?)";

    private final Session session;

    public InsertQuery(Session session) {
      this.session = session;
    }

    @Override
    public String getFormat() {
      return INSERT_SESSION_QUERY;
    }

    @Override
    public void query(PreparedStatement statement) throws SQLException {
      statement.setString(1, session.getSessionId().toString());
      statement.setString(2, session.getPlayerId().toString());
      statement.setBoolean(3, session.isDisguised());
      statement.setString(4, session.getServerName());
      statement.setLong(5, session.getStartDate().toEpochMilli());
      statement.setObject(
          6, session.getEndDate() == null ? null : session.getEndDate().toEpochMilli());
      statement.executeUpdate();
    }
  }

  private class SelectLatestSessionQuery implements Query {

    private static final String SELECT_DISGUISED_QUERY =
        "SELECT * from "
            + TABLE_NAME
            + " where player = ? AND disguised = 0 ORDER BY -end_time LIMIT 1";
    private static final String SELECT_QUERY =
        "SELECT * from " + TABLE_NAME + " where player = ? ORDER BY -end_time LIMIT 1";

    private final UUID target;
    private final boolean ignoreDisguised;

    private Session session;
    private boolean fetched;

    public SelectLatestSessionQuery(UUID target, boolean ignoreDisguised) {
      this.target = target;
      this.ignoreDisguised = ignoreDisguised;
    }

    public Session getSession() {
      return session;
    }

    public boolean hasFetched() {
      return fetched;
    }

    @Override
    public String getFormat() {
      return ignoreDisguised ? SELECT_DISGUISED_QUERY : SELECT_QUERY;
    }

    @Override
    public void query(PreparedStatement statement) throws SQLException {
      if (fetched) return;

      statement.setString(1, target.toString());

      try (final ResultSet result = statement.executeQuery()) {
        if (result.next()) {
          String id = result.getString("id");

          String player = result.getString("player");
          boolean disguised = result.getBoolean("disguised");

          String server = result.getString("server");

          long startTime = result.getLong("start_time");
          Object endTime = result.getObject("end_time");

          session =
              new Session(
                  UUID.fromString(id),
                  UUID.fromString(player),
                  disguised,
                  server,
                  Instant.ofEpochMilli(startTime),
                  endTime == null ? null : Instant.ofEpochMilli((Long) endTime));
        }
      }

      fetched = true;
    }

    public void invalidate() {
      fetched = false;
      session = null;
    }

    public void setSession(Session session) {
      fetched = true;
      this.session = session;
    }
  }

  private class UpdateSessionEndTimeQuery implements Query {

    private static final String UPDATE_QUERY =
        "UPDATE " + TABLE_NAME + " SET end_time = ? WHERE id = ?";

    private final Session session;

    public UpdateSessionEndTimeQuery(Session session) {
      this.session = session;
    }

    @Override
    public String getFormat() {
      return UPDATE_QUERY;
    }

    @Override
    public void query(PreparedStatement statement) throws SQLException {
      statement.setObject(
          1, session.getEndDate() == null ? null : session.getEndDate().toEpochMilli());
      statement.setString(2, session.getSessionId().toString());
      statement.executeUpdate();
    }
  }
}

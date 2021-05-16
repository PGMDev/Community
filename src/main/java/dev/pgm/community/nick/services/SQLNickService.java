package dev.pgm.community.nick.services;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import dev.pgm.community.database.DatabaseConnection;
import dev.pgm.community.feature.SQLFeatureBase;
import dev.pgm.community.nick.Nick;
import dev.pgm.community.nick.NickConfig;
import dev.pgm.community.nick.NickImpl;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import tc.oc.pgm.util.concurrent.ThreadSafeConnection.Query;

public class SQLNickService extends SQLFeatureBase<Nick> {

  private static final String TABLE_NAME = "nicknames";
  private static final String TABLE_FIELDS =
      "(playerId VARCHAR(36) PRIMARY KEY, nickname VARCHAR(16), date LONG, enabled BOOL)";

  private LoadingCache<UUID, SelectTargetNickQuery> nickCache;

  public SQLNickService(DatabaseConnection connection, NickConfig config) {
    super(connection, TABLE_NAME, TABLE_FIELDS);

    this.nickCache =
        CacheBuilder.newBuilder()
            .build(
                new CacheLoader<UUID, SelectTargetNickQuery>() {
                  @Override
                  public SelectTargetNickQuery load(UUID key) throws Exception {
                    return new SelectTargetNickQuery(key);
                  }
                });
  }

  @Override
  public void save(Nick nick) {
    getDatabase().submitQuery(new InsertQuery(nick));
  }

  @Override
  public CompletableFuture<List<Nick>> queryList(String target) {
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public CompletableFuture<Nick> query(String target) {
    SelectTargetNickQuery query = nickCache.getUnchecked(UUID.fromString(target));

    if (query.hasFetched()) {
      return CompletableFuture.completedFuture(query.getNick());
    } else {
      return getDatabase()
          .submitQueryComplete(query)
          .thenApplyAsync(q -> SelectTargetNickQuery.class.cast(q).getNick());
    }
  }

  public CompletableFuture<Boolean> update(Nick nick) {
    return getDatabase()
        .submitQueryComplete(new UpdateTargetNickQuery(nick))
        .thenApplyAsync(q -> UpdateTargetNickQuery.class.cast(q).isSuccessful());
  }

  public CompletableFuture<Boolean> isNameAvailable(String name) {
    return queryByName(name).thenApplyAsync(results -> results == null);
  }

  public CompletableFuture<Nick> queryByName(String name) {
    return getDatabase()
        .submitQueryComplete(new SelectNickByNameQuery(name))
        .thenApplyAsync(q -> SelectNickByNameQuery.class.cast(q).getResult());
  }

  private class InsertQuery implements Query {

    private static final String INSERT_NICKNAME_QUERY =
        "INSERT INTO " + TABLE_NAME + "(playerId, nickname, date, enabled) VALUES (?,?,?,?)";

    private final Nick nick;

    public InsertQuery(Nick nick) {
      this.nick = nick;

      SelectTargetNickQuery cached = nickCache.getUnchecked(nick.getPlayerId());
      if (cached.getNick() == null) {
        cached.nick = nick;
      }
    }

    @Override
    public String getFormat() {
      return INSERT_NICKNAME_QUERY;
    }

    @Override
    public void query(PreparedStatement statement) throws SQLException {
      statement.setString(1, nick.getPlayerId().toString());
      statement.setString(2, nick.getName());
      statement.setLong(3, nick.getDateSet().toEpochMilli());
      statement.setBoolean(4, nick.isEnabled());
      statement.executeUpdate();
    }
  }

  private class SelectTargetNickQuery implements Query {

    private static final String SELECT_QUERY =
        "SELECT * from " + TABLE_NAME + " where playerId = ? LIMIT 1";

    private final UUID target;

    private Nick nick;

    private boolean fetched;

    public SelectTargetNickQuery(UUID target) {
      this.target = target;
      this.fetched = false;
    }

    public boolean hasFetched() {
      return fetched;
    }

    public Nick getNick() {
      return nick;
    }

    public void setNick(Nick nick) {
      this.nick = nick;
    }

    @Override
    public String getFormat() {
      return SELECT_QUERY;
    }

    @Override
    public void query(PreparedStatement statement) throws SQLException {
      statement.setString(1, target.toString());
      try (final ResultSet result = statement.executeQuery()) {
        while (result.next()) {
          UUID playerId = UUID.fromString(result.getString("playerId"));
          String nickName = result.getString("nickname");
          Instant date = Instant.ofEpochMilli(result.getLong("date"));
          boolean enabled = result.getBoolean("enabled");
          setNick(new NickImpl(playerId, nickName, date, enabled));
        }
        fetched = true;
      }
    }
  }

  private class UpdateTargetNickQuery implements Query {

    private static final String UPDATE_QUERY =
        "UPDATE " + TABLE_NAME + " set nickName = ?, enabled = ?, date = ? where playerId = ?";

    private final Nick nick;
    private boolean success;

    public UpdateTargetNickQuery(Nick nick) {
      this.nick = nick;
    }

    public boolean isSuccessful() {
      return success;
    }

    @Override
    public String getFormat() {
      return UPDATE_QUERY;
    }

    @Override
    public void query(PreparedStatement statement) throws SQLException {
      statement.setString(1, nick.getName());
      statement.setBoolean(2, nick.isEnabled());
      statement.setLong(3, nick.getDateSet().toEpochMilli());
      statement.setString(4, nick.getPlayerId().toString());
      success = statement.executeUpdate() != 0;
    }
  }

  private class SelectNickByNameQuery implements Query {

    private static final String SELECT_QUERY =
        "SELECT * from " + TABLE_NAME + " where LOWER(nickname) = LOWER(?)";

    private final String name;

    private @Nullable Nick nick;

    public SelectNickByNameQuery(String targetName) {
      this.name = targetName;
      this.nick = null;
    }

    public Nick getResult() {
      return nick;
    }

    @Override
    public String getFormat() {
      return SELECT_QUERY;
    }

    @Override
    public void query(PreparedStatement statement) throws SQLException {
      statement.setString(1, name);
      try (final ResultSet result = statement.executeQuery()) {
        while (result.next()) {
          UUID playerId = UUID.fromString(result.getString("playerId"));
          String nickName = result.getString("nickname");
          Instant date = Instant.ofEpochMilli(result.getLong("date"));
          boolean enabled = result.getBoolean("enabled");
          nick = new NickImpl(playerId, nickName, date, enabled);
        }
      }
    }
  }
}

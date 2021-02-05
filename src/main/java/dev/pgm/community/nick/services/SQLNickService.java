package dev.pgm.community.nick.services;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
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
import tc.oc.pgm.util.concurrent.ThreadSafeConnection.Query;

public class SQLNickService extends SQLFeatureBase<Nick> {

  private static final String TABLE_NAME = "nick";
  private static final String TABLE_FIELDS =
      "(id VARCHAR(36) PRIMARY KEY, nickname VARCHAR(16), playerId VARCHAR(36), date LONG, valid BOOL, enabled BOOL)";

  private LoadingCache<UUID, SelectTargetNicksQuery> nickCache;

  public SQLNickService(DatabaseConnection connection, NickConfig config) {
    super(connection, TABLE_NAME, TABLE_FIELDS);

    this.nickCache =
        CacheBuilder.newBuilder()
            .build(
                new CacheLoader<UUID, SelectTargetNicksQuery>() {
                  @Override
                  public SelectTargetNicksQuery load(UUID key) throws Exception {
                    return new SelectTargetNicksQuery(key);
                  }
                });
  }

  @Override
  public void save(Nick nick) {
    getDatabase().submitQuery(new InsertQuery(nick));
  }

  @Override
  public CompletableFuture<List<Nick>> queryList(String target) {
    SelectTargetNicksQuery query = nickCache.getUnchecked(UUID.fromString(target));

    if (query.hasFetched()) {
      return CompletableFuture.completedFuture(query.getNicks());
    } else {
      return getDatabase()
          .submitQueryComplete(query)
          .thenApplyAsync(
              q ->
                  SelectTargetNicksQuery.class
                      .cast(q)
                      .getNicks()); // Gets a list of ALL nicknames a player has used
    }
  }

  @Override
  public CompletableFuture<Nick> query(String target) {
    return queryList(target)
        .thenApplyAsync(list -> list.stream().filter(Nick::isValid).findAny().orElse(null));
  }

  public CompletableFuture<Boolean> update(Nick nick) {
    return getDatabase()
        .submitQueryComplete(new UpdateTargetNickQuery(nick))
        .thenApplyAsync(q -> UpdateTargetNickQuery.class.cast(q).isSuccessful());
  }

  public CompletableFuture<Boolean> isNameAvailable(String name) {
    return queryByName(name, true).thenApplyAsync(results -> results.isEmpty());
  }

  public CompletableFuture<List<Nick>> queryByName(String name, boolean valid) {
    return getDatabase()
        .submitQueryComplete(new SelectNickByNameQuery(name, valid))
        .thenApplyAsync(q -> SelectNickByNameQuery.class.cast(q).getResults());
  }

  private class InsertQuery implements Query {

    private static final String INSERT_NICKNAME_QUERY =
        "INSERT INTO "
            + TABLE_NAME
            + "(id, nickname, playerId, date, valid, enabled) VALUES (?,?,?,?,?,?)";

    private final Nick nick;

    public InsertQuery(Nick nick) {
      this.nick = nick;

      SelectTargetNicksQuery query = nickCache.getUnchecked(nick.getPlayerId());
      if (query.hasFetched()) {
        query.getNicks().add(nick);
      }
    }

    @Override
    public String getFormat() {
      return INSERT_NICKNAME_QUERY;
    }

    @Override
    public void query(PreparedStatement statement) throws SQLException {
      statement.setString(1, nick.getNickId().toString());
      statement.setString(2, nick.getNickName());
      statement.setString(3, nick.getPlayerId().toString());
      statement.setLong(4, nick.getDateSet().toEpochMilli());
      statement.setBoolean(5, nick.isValid());
      statement.setBoolean(6, nick.isEnabled());
      statement.executeUpdate();
    }
  }

  private class SelectTargetNicksQuery implements Query {

    private static final String SELECT_QUERY =
        "SELECT * from " + TABLE_NAME + " where playerId = ?";

    private final UUID target;

    private List<Nick> nicks;

    private boolean fetched;

    public SelectTargetNicksQuery(UUID target) {
      this.target = target;
      this.nicks = Lists.newArrayList();
      this.fetched = false;
    }

    public boolean hasFetched() {
      return fetched;
    }

    public List<Nick> getNicks() {
      return nicks;
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
          UUID nickId = UUID.fromString(result.getString("id"));
          UUID playerId = UUID.fromString(result.getString("playerId"));
          String nickName = result.getString("nickname");
          Instant date = Instant.ofEpochMilli(result.getLong("date"));
          boolean valid = result.getBoolean("valid");
          boolean enabled = result.getBoolean("enabled");
          nicks.add(new NickImpl(nickId, playerId, nickName, date, valid, enabled));
        }
        fetched = true;
      }
    }
  }

  private class UpdateTargetNickQuery implements Query {

    private static final String UPDATE_QUERY =
        "UPDATE " + TABLE_NAME + " set valid = ?, enabled = ? where id = ?";

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
      statement.setBoolean(1, nick.isValid());
      statement.setBoolean(2, nick.isEnabled());
      statement.setString(3, nick.getNickId().toString());
      success = statement.executeUpdate() != 0;
    }
  }

  private class SelectNickByNameQuery implements Query {

    private static final String SELECT_QUERY =
        "SELECT * from " + TABLE_NAME + " where LOWER(nickname) = LOWER(?) AND valid = ?";

    private final String name;
    private final boolean valid;

    private List<Nick> result = Lists.newArrayList();

    public SelectNickByNameQuery(String targetName, boolean valid) {
      this.name = targetName;
      this.valid = valid;
    }

    public List<Nick> getResults() {
      return result;
    }

    @Override
    public String getFormat() {
      return SELECT_QUERY;
    }

    @Override
    public void query(PreparedStatement statement) throws SQLException {
      statement.setString(1, name);
      statement.setBoolean(2, valid);
      try (final ResultSet result = statement.executeQuery()) {
        while (result.next()) {
          UUID nickId = UUID.fromString(result.getString("id"));
          UUID playerId = UUID.fromString(result.getString("playerId"));
          String nickName = result.getString("nickname");
          Instant date = Instant.ofEpochMilli(result.getLong("date"));
          boolean valid = result.getBoolean("valid");
          boolean enabled = result.getBoolean("enabled");
          this.result.add(new NickImpl(nickId, playerId, nickName, date, valid, enabled));
        }
      }
    }
  }
}

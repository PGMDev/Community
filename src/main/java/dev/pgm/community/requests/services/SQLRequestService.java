package dev.pgm.community.requests.services;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import dev.pgm.community.database.DatabaseConnection;
import dev.pgm.community.feature.SQLFeatureBase;
import dev.pgm.community.requests.RequestProfile;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import tc.oc.pgm.util.concurrent.ThreadSafeConnection.Query;

public class SQLRequestService extends SQLFeatureBase<RequestProfile> {

  private static final String TABLE_FIELDS =
      "(id VARCHAR(36) PRIMARY KEY, last_request_time LONG, last_request_map VARCHAR(255), last_sponsor_time LONG, last_sponsor_map VARCHAR(255), tokens INT, last_token_refresh LONG)";
  private static final String TABLE_NAME = "requests";

  private LoadingCache<UUID, SelectProfileQuery> profileCache;

  public SQLRequestService(DatabaseConnection database) {
    super(database, TABLE_NAME, TABLE_FIELDS);

    this.profileCache =
        CacheBuilder.newBuilder()
            .build(
                new CacheLoader<UUID, SelectProfileQuery>() {
                  @Override
                  public SelectProfileQuery load(UUID key) throws Exception {
                    return new SelectProfileQuery(key);
                  }
                });
  }

  public CompletableFuture<RequestProfile> login(UUID playerId) {
    return query(playerId.toString())
        .thenApplyAsync(
            profile -> {
              RequestProfile reqProfile = new RequestProfile(playerId);
              if (profile == null) {
                save(reqProfile);
                return reqProfile;
              }
              return profile;
            });
  }

  @Nullable
  public RequestProfile getCached(UUID playerId) {
    SelectProfileQuery query = profileCache.getIfPresent(playerId);
    if (query != null) {
      return query.getProfile();
    }
    return null;
  }

  public void update(RequestProfile profile) {
    getDatabase().submitQuery(new UpdateProfileQuery(profile));
  }

  @Override
  public void save(RequestProfile profile) {
    getDatabase().submitQuery(new InsertQuery(profile));
    profileCache.invalidate(profile.getPlayerId());
  }

  @Override
  public CompletableFuture<List<RequestProfile>> queryList(String target) {
    return CompletableFuture.completedFuture(Lists.newArrayList());
  }

  @Override
  public CompletableFuture<RequestProfile> query(String target) {
    SelectProfileQuery query = profileCache.getUnchecked(UUID.fromString(target));
    if (query.getProfile() != null) {
      return CompletableFuture.completedFuture(query.getProfile());
    } else {
      return getDatabase()
          .submitQueryComplete(query)
          .thenApplyAsync(
              q -> {
                if (q instanceof SelectProfileQuery) {
                  return SelectProfileQuery.class.cast(q).getProfile();
                }
                return null;
              });
    }
  }

  private long convertTime(Instant time) {
    if (time == null) {
      return -1;
    }
    return time.toEpochMilli();
  }

  private class InsertQuery implements Query {

    private static final String INSERT_QUERY =
        "INSERT INTO " + TABLE_NAME + " VALUES (?,?,?,?,?,?,?)";

    private RequestProfile profile;

    public InsertQuery(RequestProfile profile) {
      this.profile = profile;
    }

    @Override
    public String getFormat() {
      return INSERT_QUERY;
    }

    @Override
    public void query(PreparedStatement statement) throws SQLException {
      statement.setString(1, profile.getPlayerId().toString());
      statement.setLong(2, convertTime(profile.getLastRequestTime()));
      statement.setString(3, profile.getLastRequestMap());
      statement.setLong(4, convertTime(profile.getLastSponsorTime()));
      statement.setString(5, profile.getLastSponsorMap());
      statement.setInt(6, profile.getSponsorTokens());
      statement.setLong(7, convertTime(profile.getLastTokenRefreshTime()));
      statement.execute();
    }
  }

  private class SelectProfileQuery implements Query {
    private static final String FORMAT = "SELECT * from " + TABLE_NAME + " WHERE id = ? LIMIT 1";

    private UUID playerId;
    private RequestProfile profile;

    public SelectProfileQuery(UUID playerId) {
      this.playerId = playerId;
      this.profile = null;
    }

    @Override
    public String getFormat() {
      return FORMAT;
    }

    public RequestProfile getProfile() {
      return profile;
    }

    @Override
    public void query(PreparedStatement statement) throws SQLException {
      statement.setString(1, playerId.toString());
      try (final ResultSet result = statement.executeQuery()) {
        if (!result.next()) {
          return;
        }

        final UUID id = UUID.fromString(result.getString("id"));
        final long lastRequest = result.getLong("last_request_time");
        final String lastRequestMap = result.getString("last_request_map");
        final long lastSponsor = result.getLong("last_sponsor_time");
        final String lastSponsorMap = result.getString("last_sponsor_map");
        final int tokens = result.getInt("tokens");
        final long lastToken = result.getLong("last_token_refresh");

        final Instant lastRequestTime =
            lastRequest == -1 ? null : Instant.ofEpochMilli(lastRequest);
        final Instant lastSponsorTime =
            lastSponsor == -1 ? null : Instant.ofEpochMilli(lastSponsor);
        final Instant lastTokenRefreshTime =
            lastToken == -1 ? null : Instant.ofEpochMilli(lastToken);

        this.profile =
            new RequestProfile(
                id,
                lastRequestTime,
                lastRequestMap,
                lastSponsorTime,
                lastSponsorMap,
                tokens,
                lastTokenRefreshTime);
      }
    }
  }

  private class UpdateProfileQuery implements Query {
    private static final String FORMAT =
        "UPDATE "
            + TABLE_NAME
            + " SET last_request_time = ?, last_request_map = ?, last_sponsor_time = ?, last_sponsor_map = ?, tokens = ?, last_token_refresh = ? WHERE id = ? ";

    private RequestProfile profile;

    public UpdateProfileQuery(RequestProfile profile) {
      this.profile = profile;
    }

    @Override
    public String getFormat() {
      return FORMAT;
    }

    @Override
    public void query(PreparedStatement statement) throws SQLException {
      statement.setLong(1, convertTime(profile.getLastRequestTime()));
      statement.setString(2, profile.getLastRequestMap());
      statement.setLong(3, convertTime(profile.getLastSponsorTime()));
      statement.setString(4, profile.getLastSponsorMap());
      statement.setInt(5, profile.getSponsorTokens());
      statement.setLong(6, convertTime(profile.getLastTokenRefreshTime()));
      statement.setString(7, profile.getPlayerId().toString());
      statement.executeUpdate();
    }
  }
}

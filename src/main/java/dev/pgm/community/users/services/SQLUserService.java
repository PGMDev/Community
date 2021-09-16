package dev.pgm.community.users.services;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import dev.pgm.community.database.DatabaseConnection;
import dev.pgm.community.feature.SQLFeatureBase;
import dev.pgm.community.users.UserProfile;
import dev.pgm.community.users.UserProfileImpl;
import dev.pgm.community.users.feature.UsersFeature;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import tc.oc.pgm.util.concurrent.ThreadSafeConnection.Query;

public class SQLUserService extends SQLFeatureBase<UserProfile, String> {

  private static final String TABLE_FIELDS =
      "(id VARCHAR(36) PRIMARY KEY, name VARCHAR(16), first_join LONG, join_count INT)";
  private static final String TABLE_NAME = "users";

  private LoadingCache<UUID, SelectProfileQuery> profileCache;

  public SQLUserService(DatabaseConnection database) {
    super(database, TABLE_NAME, TABLE_FIELDS);

    this.profileCache =
        CacheBuilder.newBuilder()
            .build(
                new CacheLoader<UUID, SelectProfileQuery>() {
                  @Override
                  public SelectProfileQuery load(UUID key) throws Exception {
                    return new SelectProfileQuery(key.toString());
                  }
                });
  }

  @Override
  public void save(UserProfile profile) {
    getDatabase().submitQuery(new InsertQuery(profile));
    profileCache.invalidate(profile.getId());
  }

  @Override
  public CompletableFuture<List<UserProfile>> queryList(String target) {
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public CompletableFuture<UserProfile> query(String target) {
    SelectProfileQuery query = null;

    // If Username, search through looking for existing usernames in profiles
    if (UsersFeature.USERNAME_REGEX.matcher(target).matches()) {
      Optional<SelectProfileQuery> uQuery =
          profileCache.asMap().values().stream()
              .filter(q -> q.getQuery().equalsIgnoreCase(target))
              .findAny();

      if (uQuery.isPresent()) {
        query = uQuery.get();
      } else {
        query = new SelectProfileQuery(target);
      }

    } else {
      // Lookup via uuid
      query = profileCache.getUnchecked(UUID.fromString(target));
    }

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

  // Increase join count, set last login, check for username change
  public CompletableFuture<UserProfile> login(UUID id, String username, String address) {
    return query(id.toString())
        .thenApplyAsync(
            profile -> {
              if (profile == null) {
                // No profile? Save a new one
                profile = new UserProfileImpl(id, username);
                save(profile);
              } else {
                // Existing profile - Update name, login, joins
                profile.setUsername(username);
                profile.incJoinCount();
                getDatabase().submitQuery(new UpdateProfileQuery(profile));
              }
              return profile;
            });
  }

  public void logout(UUID id) {
    query(id.toString())
        .thenAcceptAsync(
            profile -> {
              // Set last login time when quitting
              getDatabase().submitQuery(new UpdateProfileQuery(profile));
            });
  }

  private class InsertQuery implements Query {

    private static final String INSERT_USERNAME_QUERY =
        "INSERT INTO " + TABLE_NAME + "(id, username, first_login, join_count) VALUES (?,?,?,?)";

    private UserProfile profile;

    public InsertQuery(UserProfile profile) {
      this.profile = profile;
    }

    @Override
    public String getFormat() {
      return INSERT_USERNAME_QUERY;
    }

    @Override
    public void query(PreparedStatement statement) throws SQLException {
      statement.setString(1, profile.getId().toString());
      statement.setString(2, profile.getUsername());
      statement.setLong(3, profile.getFirstLogin().toEpochMilli());
      statement.setInt(4, profile.getJoinCount());
      statement.execute();
    }
  }

  private class SelectProfileQuery implements Query {
    private static final String FORMAT = "SELECT * from " + TABLE_NAME + " WHERE ";

    private String query;
    private UserProfile profile;

    public SelectProfileQuery(String query) {
      this.query = query;
    }

    @Override
    public String getFormat() {
      return FORMAT + (isUsernameQuery() ? "LOWER(name) = LOWER(?)" : "id = ?") + " LIMIT 1";
    }

    public String getQuery() {
      return query;
    }

    private boolean isUsernameQuery() {
      return UsersFeature.USERNAME_REGEX.matcher(query).matches();
    }

    public UserProfile getProfile() {
      return profile;
    }

    @Override
    public void query(PreparedStatement statement) throws SQLException {
      statement.setString(1, query);
      try (final ResultSet result = statement.executeQuery()) {
        if (!result.next()) {
          return;
        }

        final UUID id = UUID.fromString(result.getString("id"));
        final String username = result.getString("name");
        final long firstJoin = result.getLong("first_join");
        final int joinCount = result.getInt("join_count");
        this.profile =
            new UserProfileImpl(id, username, Instant.ofEpochMilli(firstJoin), joinCount);
      }
    }
  }

  private class UpdateProfileQuery implements Query {
    private static final String FORMAT =
        "UPDATE " + TABLE_NAME + " SET name = ?, join_count = ? WHERE id = ? ";

    private UserProfile profile;

    public UpdateProfileQuery(UserProfile profile) {
      this.profile = profile;
    }

    @Override
    public String getFormat() {
      return FORMAT;
    }

    @Override
    public void query(PreparedStatement statement) throws SQLException {
      statement.setString(1, profile.getUsername());
      statement.setInt(2, profile.getJoinCount());
      statement.setString(3, profile.getId().toString());
      statement.executeUpdate();
    }
  }
}

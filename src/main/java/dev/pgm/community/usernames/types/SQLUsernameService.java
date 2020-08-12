package dev.pgm.community.usernames.types;

import com.google.common.collect.Maps;
import dev.pgm.community.database.DatabaseConnection;
import dev.pgm.community.database.query.TableQuery;
import dev.pgm.community.usernames.UsernameService;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import tc.oc.pgm.util.concurrent.ThreadSafeConnection.Query;

/** SQLUsername A cached username service with a SQLITE or MYSQL backend */
public class SQLUsernameService extends CachedUsernameService {

  // NOTE: Is there a benefit to expiring usernames?
  // If we will be tracking punishments/other small stats long term, is it better to keep all names
  // who join?

  private static final String TABLE_FIELDS = "(id VARCHAR(36) PRIMARY KEY, name VARCHAR(16))";
  private static final String TABLE_NAME = "usernames";

  private DatabaseConnection database;

  public SQLUsernameService(Logger logger, DatabaseConnection database) {
    super(logger);
    this.database = database;
    database.submitQuery(new TableQuery(TABLE_NAME, TABLE_FIELDS));
  }

  @Override
  public CompletableFuture<String> getStoredUsername(UUID id) {
    String cached = super.getUsername(id);

    if (cached == null) {
      return database
          .submitQueryComplete(new SelectQuery(id.toString()))
          .thenApplyAsync(
              q -> {
                String name = SelectQuery.class.cast(q).getResult();
                if (name != null) {
                  super.setName(id, name);
                }
                return super.getUsername(id);
              });
    }

    return super.getStoredUsername(id);
  }

  @Override
  public CompletableFuture<Optional<UUID>> getStoredId(String username) {
    Optional<UUID> cached = super.getId(username);
    if (cached.isPresent()) {
      return super.getStoredId(username);
    } else {
      return database
          .submitQueryComplete(new SelectQuery(username))
          .thenApplyAsync(
              q -> {
                String res = SelectQuery.class.cast(q).getResult();
                if (res != null) {
                  super.setName(UUID.fromString(res), username);
                }
                return super.getId(username);
              });
    }
  }

  @Override
  public void setName(UUID uuid, String name) {
    getStoredUsername(uuid)
        .thenAcceptAsync(
            stored -> {
              if (stored == null || !name.equalsIgnoreCase(stored)) {
                database.submitQuery(new InsertQuery(uuid, name));
                logger.info(
                    String.format("Username: Updated %s to %s in database", uuid.toString(), name));
              }
              super.setName(uuid, name); // Save to cache for instant lookup
              logger.info(String.format("Stored Username: %s", name));
            });
  }

  @Override
  public CompletableFuture<Map<UUID, String>> getStoredNamesDebug() {
    return database
        .submitQueryComplete(new DebugNamesQuery())
        .thenApplyAsync(q -> DebugNamesQuery.class.cast(q).getResults());
  }

  private class DebugNamesQuery implements Query {

    private Map<UUID, String> results = Maps.newHashMap();

    public Map<UUID, String> getResults() {
      return results;
    }

    @Override
    public String getFormat() {
      return "SELECT * from usernames";
    }

    @Override
    public void query(PreparedStatement statement) throws SQLException {
      try (final ResultSet result = statement.executeQuery()) {
        while (result.next()) {
          UUID id = UUID.fromString(result.getString("id"));
          String name = result.getString("name");
          results.put(id, name);
        }
      }
    }
  }

  private class InsertQuery implements Query {

    private static final String INSERT_USERNAME_QUERY =
        "REPLACE INTO " + TABLE_NAME + " VALUES (?,?)";

    private UUID uuid;
    private String username;

    public InsertQuery(UUID uuid, String username) {
      this.uuid = uuid;
      this.username = username;
    }

    @Override
    public String getFormat() {
      return INSERT_USERNAME_QUERY;
    }

    @Override
    public void query(PreparedStatement statement) throws SQLException {
      statement.setString(1, uuid.toString());
      statement.setString(2, username);
      statement.execute();
    }
  }

  private class SelectQuery implements Query {
    private static final String NAME_QUERY = // For finding username
        "SELECT name FROM usernames WHERE id = ? LIMIT 1";
    private static final String UUID_QUERY = // For finding UUID
        "SELECT id from usernames WHERE name = ? LIMIT 1";

    private String target;
    private String value;

    public SelectQuery(String target) {
      this.target = target;
      this.value = null;
    }

    public String getResult() {
      return value;
    }

    @Override
    public String getFormat() {
      return isUsernameQuery() ? UUID_QUERY : NAME_QUERY;
    }

    private boolean isUsernameQuery() {
      return UsernameService.USERNAME_REGEX.matcher(target).matches();
    }

    @Override
    public void query(PreparedStatement statement) throws SQLException {
      statement.setString(1, target);

      try (final ResultSet result = statement.executeQuery()) {
        if (!result.next()) return;
        this.value = result.getString(1);
      }
    }
  }
}

package dev.pgm.community.usernames.types;

import co.aikar.idb.DB;
import com.google.common.collect.Maps;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/** SQLUsername A cached username service with a SQLITE or MYSQL backend */
public class SQLUsernameService extends CachedUsernameService {

  // NOTE: Is there a benefit to expiring usernames?
  // If we will be tracking punishments/other small stats long term, is it better to keep all names
  // who join?

  private static final String SELECT_USERNAME_QUERY =
      "SELECT name FROM usernames WHERE id = ? LIMIT 1";
  private static final String SELECT_UUID_QUERY = "SELECT id from usernames WHERE name = ? LIMIT 1";
  private static final String UPDATE_USERNAME_QUERY = "REPLACE INTO usernames VALUES (?,?)";
  private static final String CREATE_TABLE_QUERY =
      "CREATE TABLE IF NOT EXISTS %s (id VARCHAR(36) PRIMARY KEY, name VARCHAR(16))";
  private static final String TABLE_NAME = "usernames";

  public SQLUsernameService(Logger logger) {
    super(logger);
    createTable();
    try {
      logger.info("Total cached usernames: " + DB.getResults("SELECT * from usernames").size());
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  private void createTable() {
    try {
      DB.executeUpdate(String.format(CREATE_TABLE_QUERY, TABLE_NAME));
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  @Override
  public CompletableFuture<String> getStoredUsername(UUID id) {
    logger.info("Get stored username has started...");
    String cached = super.getUsername(id);

    if (cached == null) {
      return DB.getFirstRowAsync(SELECT_USERNAME_QUERY, id.toString())
          .thenApply(
              result -> (result == null || result.isEmpty() ? null : result.getString("name")));
    }

    return super.getStoredUsername(id);
  }

  @Override
  public CompletableFuture<Optional<UUID>> getStoredId(String username) {
    Optional<UUID> cached = super.getId(username);
    if (cached.isPresent()) {
      return super.getStoredId(username);
    } else {
      return DB.getFirstRowAsync(SELECT_UUID_QUERY, username)
          .thenApply(
              result -> {
                Optional<UUID> id = Optional.empty();
                if (!result.isEmpty()) {
                  Optional.of(result.getString("id"));
                }
                return id;
              });
    }
  }

  @Override
  public void setName(UUID uuid, String name) {
    logger.info("setName has been called.");
    getStoredUsername(uuid)
        .thenAcceptAsync(
            stored -> {
              if (stored == null || !name.equalsIgnoreCase(stored)) {
                DB.executeUpdateAsync(UPDATE_USERNAME_QUERY, uuid.toString(), name);
                logger.info(String.format("Username: Updated %s to %s in database", uuid.toString(), name));
              }
              super.setName(uuid, name); // Save to cache for instant lookup
              logger.info(String.format("Stored Username: %s", name));
            });
  }

  @Override
  public CompletableFuture<Map<UUID, String>> getStoredNamesDebug() {
    return DB.getResultsAsync("SELECT * from usernames")
        .thenApplyAsync(
            rows -> {
              Map<UUID, String> results = Maps.newHashMap();
              rows.forEach(
                  result -> {
                    UUID id = UUID.fromString(result.getString("id"));
                    String name = result.getString("name");
                    results.put(id, name);
                  });
              return results;
            });
  }
}

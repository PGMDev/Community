package dev.pgm.community.database;

import org.bukkit.configuration.Configuration;

public class DatabaseConfig {

  private boolean enabled;
  private String username;
  private String password;
  private String host;
  private String databaseName;
  private String timezone;
  private int maxConnections;

  public DatabaseConfig(Configuration config) {
    reload(config);
  }

  public void reload(Configuration config) {
    this.enabled = config.getBoolean("database.enabled", true);
    this.username = config.getString("database.username");
    this.password = config.getString("database.password");
    this.host = config.getString("database.host");
    this.databaseName = config.getString("database.databaseName");
    this.timezone = config.getString("database.timezone");
    this.maxConnections = config.getInt("database.max-connections");
  }

  public boolean isEnabled() {
    return enabled;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public String getHost() {
    return host;
  }

  public String getDatabaseName() {
    return databaseName;
  }

  public String getTimezone() {
    return timezone;
  }

  public int getMaxDatabaseConnections() {
    return maxConnections;
  }
}

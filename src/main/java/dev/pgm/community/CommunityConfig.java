package dev.pgm.community;

import org.bukkit.configuration.Configuration;

public class CommunityConfig {

  private DatabaseType database;

  // SQL Options
  private String sqlFileName;

  // MySQL Options
  private String address;
  private String username;
  private String password;
  private String table;

  public CommunityConfig(Configuration config) {
    reload(config);
  }

  public void reload(Configuration config) {
    this.database = DatabaseType.of(config.getString("database-mode"));
    this.sqlFileName = config.getString("sql-name");
    this.address = config.getString("mysql.address");
    this.username = config.getString("mysql.username");
    this.password = config.getString("mysql.password");
    this.table = config.getString("mysql.table");
  }

  public DatabaseType getDatabaseType() {
    return database;
  }

  public String getSqlFileName() {
    return sqlFileName;
  }

  public String getDatabaseAddress() {
    return address;
  }

  public String getDatabaseUsername() {
    return username;
  }

  public String getDatabasePassword() {
    return password;
  }

  public String getDatabaseTable() {
    return table;
  }

  public static enum DatabaseType {
    SQL,
    MYSQL,
    NONE;

    public static DatabaseType of(String value) {
      switch (value.toLowerCase()) {
        case "sql":
        case "sqlite":
        case "file":
          return SQL;
        case "mysql":
        case "remote":
          return MYSQL;
        default:
          return NONE;
      }
    }
  }
}

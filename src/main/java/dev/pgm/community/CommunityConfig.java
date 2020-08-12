package dev.pgm.community;

import java.io.File;
import org.bukkit.configuration.Configuration;

public class CommunityConfig {

  private String serverDisplayName;

  private boolean databaseEnabled;
  private String databaseUri;
  private int databaseMaxConnections;

  public CommunityConfig(Configuration config) {
    reload(config);
  }

  public void reload(Configuration config) {
    this.serverDisplayName = config.getString("general.server-name", "");
    this.databaseEnabled = config.getBoolean("database.enabled", true);
    this.databaseUri = config.getString("database.uri", "");

    if (databaseUri == null || databaseUri.isEmpty()) {
      this.databaseUri =
          new File(Community.get().getDataFolder(), "community.db")
              .getAbsoluteFile()
              .toURI()
              .toString()
              .replaceFirst("^file", "sqlite");
    }

    this.databaseMaxConnections =
        this.databaseUri.startsWith("sqlite:")
            ? 1
            : Math.min(5, Runtime.getRuntime().availableProcessors());
  }

  public boolean isDatabaseEnabled() {
    return databaseEnabled;
  }

  public String getDatabaseUri() {
    return databaseUri;
  }

  public int getMaxDatabaseConnections() {
    return databaseMaxConnections;
  }

  public String getServerDisplayName() {
    return serverDisplayName;
  }
}

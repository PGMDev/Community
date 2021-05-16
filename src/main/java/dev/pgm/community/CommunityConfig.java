package dev.pgm.community;

import dev.pgm.community.utils.NetworkUtils;
import java.io.File;
import org.bukkit.configuration.Configuration;

public class CommunityConfig {

  private String serverDisplayName;
  private String serverId;

  private boolean databaseEnabled;
  private String databaseUri;
  private int databaseMaxConnections;

  public CommunityConfig(Configuration config) {
    reload(config);
  }

  public void reload(Configuration config) {
    this.serverDisplayName = config.getString("general.server-name", "");
    this.serverId = config.getString("general.server-id", "");
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

  public String getServerId() {
    return NetworkUtils.getServerVar(serverId);
  }
}

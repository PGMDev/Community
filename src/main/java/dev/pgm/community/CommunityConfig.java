package dev.pgm.community;

import dev.pgm.community.utils.NetworkUtils;
import org.bukkit.configuration.Configuration;
import tc.oc.occ.environment.Environment;

public class CommunityConfig {

  private String serverDisplayName;
  private String serverId;
  private boolean useEnvironment;
  private String environmentServerIdKey;

  public CommunityConfig(Configuration config) {
    reload(config);
  }

  public void reload(Configuration config) {
    this.serverDisplayName = config.getString("general.server-name", "");
    this.serverId = config.getString("general.server-id", "");
    this.useEnvironment = config.getBoolean("general.use-environment");
    this.environmentServerIdKey = config.getString("general.environment-server-id");
  }

  public String getServerDisplayName() {
    return serverDisplayName;
  }

  public String getServerId() {
    if (useEnvironment) {
      String serverId = Environment.get().getString(environmentServerIdKey);
      if (serverId != null && !serverId.isEmpty()) {
        return serverId;
      }
    }
    return NetworkUtils.getServerVar(serverId);
  }

  public boolean isEnvironmentEnabled() {
    return useEnvironment && Environment.get() != null;
  }
}

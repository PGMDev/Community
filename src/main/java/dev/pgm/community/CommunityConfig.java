package dev.pgm.community;

import dev.pgm.community.utils.NetworkUtils;
import org.bukkit.configuration.Configuration;

public class CommunityConfig {

  private String serverDisplayName;
  private String serverId;

  public CommunityConfig(Configuration config) {
    reload(config);
  }

  public void reload(Configuration config) {
    this.serverDisplayName = config.getString("general.server-name", "");
    this.serverId = config.getString("general.server-id", "");
  }

  public String getServerDisplayName() {
    return serverDisplayName;
  }

  public String getServerId() {
    return NetworkUtils.getServerVar(serverId);
  }
}

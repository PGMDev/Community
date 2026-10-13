package dev.pgm.community;

import dev.pgm.community.utils.NetworkUtils;
import dev.pgm.community.utils.ranks.RanksConfig;
import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;
import tc.oc.occ.environment.Environment;

public class CommunityConfig {

  private String serverDisplayName;
  private String serverId;
  private RanksConfig ranks;
  private String storeLink;

  private String mojangAPIAddress;
  private String randomNameAPIAddress;

  public CommunityConfig(Configuration config) {
    reload(config);
  }

  public void reload(Configuration config) {
    this.ranks = new RanksConfig(config);
    this.serverDisplayName = config.getString("general.server-name", "");
    if (serverId == null) serverId = resolveServerId(config);
    this.storeLink = config.getString("general.store-link");
    this.mojangAPIAddress = config.getString("general.mojang-api");
    this.randomNameAPIAddress = config.getString("general.randomname-api");
  }

  public String getServerDisplayName() {
    return serverDisplayName;
  }

  public String getServerId() {
    return serverId;
  }

  private static String resolveServerId(Configuration config) {
    if (config.getBoolean("general.use-environment")
        && Bukkit.getPluginManager().isPluginEnabled("Environment")) {
      String id = Environment.get().getString(config.getString("general.environment-server-id"));
      if (id != null && !id.isEmpty()) return id;
    }
    return NetworkUtils.getServerVar(config.getString("general.server-id", ""));
  }

  public RanksConfig getRanksConfig() {
    return ranks;
  }

  public String getStoreLink() {
    return storeLink;
  }

  public String getMojangAPIAddress() {
    return mojangAPIAddress;
  }

  public String getRandomNameAPIAddress() {
    return randomNameAPIAddress;
  }
}

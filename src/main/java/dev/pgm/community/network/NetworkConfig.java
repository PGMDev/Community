package dev.pgm.community.network;

import dev.pgm.community.feature.config.FeatureConfigImpl;
import java.util.UUID;
import org.bukkit.configuration.Configuration;

public class NetworkConfig extends FeatureConfigImpl {

  private static final String KEY = "network";

  private static final String ID = KEY + ".id";

  private static final String REDIS = KEY + ".redis";
  private static final String REDIS_HOST = REDIS + ".host";
  private static final String REDIS_PASSWORD = REDIS + ".password";
  private static final String REDIS_PORT = REDIS + ".port";
  private static final String REDIS_SSL = REDIS + ".ssl";

  private String host;
  private String password;
  private int port;
  private boolean ssl;

  private String networkId;

  public NetworkConfig(Configuration config) {
    super(KEY, config);
  }

  public String getNetworkId() {
    return networkId;
  }

  public String getHost() {
    return host;
  }

  public String getPassword() {
    return password;
  }

  public int getPort() {
    return port;
  }

  public boolean isSSL() {
    return ssl;
  }

  @Override
  public void reload(Configuration config) {
    super.reload(config);
    this.host = config.getString(REDIS_HOST);
    this.password = config.getString(REDIS_PASSWORD);
    this.port = config.getInt(REDIS_PORT);
    this.ssl = config.getBoolean(REDIS_SSL);

    String netId = config.getString(ID);
    this.networkId = netId != null && !netId.isEmpty() ? netId : UUID.randomUUID().toString();
  }
}

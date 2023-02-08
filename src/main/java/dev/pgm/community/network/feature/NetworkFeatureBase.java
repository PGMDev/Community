package dev.pgm.community.network.feature;

import dev.pgm.community.feature.FeatureBase;
import dev.pgm.community.network.NetworkConfig;
import java.util.logging.Logger;
import org.bukkit.configuration.Configuration;

public abstract class NetworkFeatureBase extends FeatureBase implements NetworkFeature {

  public NetworkFeatureBase(Configuration config, Logger logger, String featureName) {
    super(new NetworkConfig(config), logger, featureName);
    if (getConfig().isEnabled()) {
      enable();
    }
  }

  public NetworkConfig getNetworkConfig() {
    return (NetworkConfig) getConfig();
  }

  @Override
  public String getNetworkId() {
    return getNetworkConfig().getNetworkId();
  }
}

package dev.pgm.community.network.feature;

import com.google.common.collect.Sets;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.feature.FeatureBase;
import dev.pgm.community.network.NetworkConfig;
import java.util.Set;
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

  @Override
  public Set<CommunityCommand> getCommands() {
    return Sets.newHashSet();
  }
}

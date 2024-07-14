package dev.pgm.community.chat.network;

import dev.pgm.community.feature.config.FeatureConfigImpl;
import org.bukkit.configuration.Configuration;

public class NetworkChatConfig extends FeatureConfigImpl {

  private static final String KEY = "network.chat";

  public NetworkChatConfig(Configuration config) {
    super(KEY, config);
  }
}

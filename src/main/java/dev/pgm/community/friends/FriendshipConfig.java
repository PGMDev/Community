package dev.pgm.community.friends;

import dev.pgm.community.feature.config.FeatureConfigImpl;
import org.bukkit.configuration.Configuration;

public class FriendshipConfig extends FeatureConfigImpl {

  public static final String KEY = "friends";

  public FriendshipConfig(Configuration config) {
    super(KEY, config);
  }

  @Override
  public void reload() {
    super.reload();
  }
}

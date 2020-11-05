package dev.pgm.community.friends;

import dev.pgm.community.feature.config.FeatureConfigImpl;
import org.bukkit.configuration.Configuration;

public class FriendshipConfig extends FeatureConfigImpl {

  public static final String KEY = "friends";

  private boolean pgmIntegration;

  public FriendshipConfig(Configuration config) {
    super(KEY, config);
  }

  public boolean isIntegrationEnabled() {
    return pgmIntegration;
  }

  @Override
  public void reload() {
    super.reload();
    this.pgmIntegration = config.getBoolean(getKey() + ".pgm-integration");
  }
}

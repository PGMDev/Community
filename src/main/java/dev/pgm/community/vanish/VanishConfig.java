package dev.pgm.community.vanish;

import dev.pgm.community.feature.config.FeatureConfigImpl;
import org.bukkit.configuration.Configuration;

public class VanishConfig extends FeatureConfigImpl {

  public static final String KEY = "vanish";

  private boolean pgmIntegration;

  public VanishConfig(Configuration config) {
    super(KEY, config);
  }

  public boolean isIntegrationEnabled() {
    return pgmIntegration;
  }

  @Override
  public void reload(Configuration config) {
    super.reload(config);
    this.pgmIntegration = config.getBoolean(getKey() + ".pgm-integration");
  }
}

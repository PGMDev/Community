package dev.pgm.community.nick;

import dev.pgm.community.feature.config.FeatureConfigImpl;
import org.bukkit.configuration.Configuration;

public class NickConfig extends FeatureConfigImpl {

  private static final String KEY = "nick";

  private boolean pgmIntegration;

  public NickConfig(Configuration config) {
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

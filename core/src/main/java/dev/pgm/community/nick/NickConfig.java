package dev.pgm.community.nick;

import dev.pgm.community.feature.config.FeatureConfigImpl;
import org.bukkit.configuration.Configuration;

public class NickConfig extends FeatureConfigImpl {

  private static final String KEY = "nick";

  private boolean pgmIntegration;
  private int randomNameCount;

  public NickConfig(Configuration config) {
    super(KEY, config);
  }

  public boolean isIntegrationEnabled() {
    return pgmIntegration;
  }

  public int getRandomNameCount() {
    return randomNameCount;
  }

  @Override
  public void reload(Configuration config) {
    super.reload(config);
    this.pgmIntegration = config.getBoolean(getKey() + ".pgm-integration");
    int configuredCount = config.getInt(getKey() + ".random-name-count", 16);
    this.randomNameCount = configuredCount > 0 ? configuredCount : 16;
  }
}

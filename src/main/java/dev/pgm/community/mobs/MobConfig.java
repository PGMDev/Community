package dev.pgm.community.mobs;

import dev.pgm.community.feature.config.FeatureConfigImpl;
import org.bukkit.configuration.Configuration;

public class MobConfig extends FeatureConfigImpl {

  private static final String KEY = "mobs";

  public MobConfig(Configuration config) {
    super(KEY, config);
  }
}

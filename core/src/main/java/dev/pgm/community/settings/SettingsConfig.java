package dev.pgm.community.settings;

import dev.pgm.community.feature.config.FeatureConfigImpl;
import org.bukkit.configuration.Configuration;

public class SettingsConfig extends FeatureConfigImpl {

  public static final String KEY = "settings";

  public SettingsConfig(Configuration config) {
    super(KEY, config);
  }
}

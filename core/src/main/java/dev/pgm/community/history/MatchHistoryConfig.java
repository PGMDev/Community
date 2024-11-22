package dev.pgm.community.history;

import dev.pgm.community.feature.config.FeatureConfigImpl;
import org.bukkit.configuration.Configuration;

public class MatchHistoryConfig extends FeatureConfigImpl {

  private static final String KEY = "history";

  public MatchHistoryConfig(Configuration config) {
    super(KEY, config);
  }
}

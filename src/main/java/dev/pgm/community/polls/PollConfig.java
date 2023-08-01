package dev.pgm.community.polls;

import dev.pgm.community.feature.config.FeatureConfigImpl;
import org.bukkit.configuration.Configuration;

public class PollConfig extends FeatureConfigImpl {

  private static final String KEY = "polls";

  public PollConfig(Configuration config) {
    super(KEY, config);
  }
}

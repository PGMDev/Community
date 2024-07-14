package dev.pgm.community.mutations;

import dev.pgm.community.feature.config.FeatureConfigImpl;
import org.bukkit.configuration.Configuration;

public class MutationConfig extends FeatureConfigImpl {

  private static final String KEY = "mutations";

  public MutationConfig(Configuration config) {
    super(KEY, config);
  }
}

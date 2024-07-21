package dev.pgm.community.squads;

import dev.pgm.community.feature.config.FeatureConfigImpl;
import org.bukkit.configuration.Configuration;

public class SquadConfig extends FeatureConfigImpl {

  private static final String KEY = "squads";

  public SquadConfig(Configuration config) {
    super(KEY, config);
  }
}

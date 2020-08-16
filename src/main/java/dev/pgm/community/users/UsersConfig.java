package dev.pgm.community.users;

import dev.pgm.community.feature.config.FeatureConfigImpl;
import org.bukkit.configuration.Configuration;

public class UsersConfig extends FeatureConfigImpl {

  public static final String KEY = "users";

  public UsersConfig(Configuration config) {
    super(KEY, config);
  }
}

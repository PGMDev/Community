package dev.pgm.community.sessions;

import dev.pgm.community.feature.config.FeatureConfigImpl;
import org.bukkit.configuration.Configuration;

public class SessionConfig extends FeatureConfigImpl {

  public static final String KEY = "users.sessions";

  public SessionConfig(Configuration config) {
    super(KEY, config);
  }

  @Override
  public void reload(Configuration config) {
    super.reload(config);
  }

  @Override
  protected String getEnabledKey(String key) {
    return "users.enabled";
  }
}

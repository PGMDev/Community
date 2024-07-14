package dev.pgm.community.feature.config;

import org.bukkit.configuration.Configuration;

/** An implementation of {@link FeatureConfig} * */
public abstract class FeatureConfigImpl implements FeatureConfig {

  private final String key;

  private boolean enabled;

  public FeatureConfigImpl(String key, Configuration config) {
    this.key = key;
    reload(config);
  }

  @Override
  public String getKey() {
    return key;
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public void setEnabled(boolean yes) {
    this.enabled = yes;
  }

  @Override
  public void reload(Configuration config) {
    this.enabled = config.getBoolean(getEnabledKey(key));
  }

  protected String getEnabledKey(String key) {
    return key + ".enabled";
  }
}

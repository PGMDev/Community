package dev.pgm.community.feature.config;

import org.bukkit.configuration.Configuration;

public abstract class FeatureConfigImpl implements FeatureConfig {

  protected final Configuration config;

  private final String key;

  private boolean enabled;

  public FeatureConfigImpl(String key, Configuration config) {
    this.key = key;
    this.config = config;
    reload();
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

  public void reload() {
    this.enabled = config.getBoolean(getEnabledKey());
  }

  private String getEnabledKey() {
    return key + ".enabled";
  }
}

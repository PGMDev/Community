package dev.pgm.community;

/** FeatureManager an abstract class that all feature managers will extend */
public abstract class FeatureManager {

  protected final String featureKey;
  protected final Config config;

  public FeatureManager(String featureKey, Config config) {
    this.featureKey = featureKey;
    this.config = config;
  }

  public boolean isEnabled() {
    return config.getFeature(featureKey).getBoolean("enabled", false);
  }
}

package dev.pgm.community.feature.config;

public interface FeatureConfig {

  /** Saves current config values to file */
  void save();

  /** Reloads config values with those from the file */
  void reload();

  /**
   * The key associated with the feature e.g "reports" for {@link ReportFeature}
   *
   * @return the string key
   */
  String getKey();

  /**
   * Enables or disables the feature
   *
   * @param yes whether to enable or disable
   */
  void setEnabled(boolean yes);

  /**
   * Gets whether the feature is enabled or disabled
   *
   * @return whether enabled
   */
  boolean isEnabled();
}

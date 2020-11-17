package dev.pgm.community.feature.config;

import dev.pgm.community.reports.feature.ReportFeature;
import org.bukkit.configuration.Configuration;

public interface FeatureConfig {

  /** Reloads config values with those from the file */
  void reload(Configuration config);

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

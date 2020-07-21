package dev.pgm.community.feature;

import dev.pgm.community.feature.config.FeatureConfig;

public interface Feature {

  boolean isEnabled();

  void setEnabled(boolean on);

  void enable();

  void disable();

  FeatureConfig getConfig();
}

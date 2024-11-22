package dev.pgm.community.feature;

import dev.pgm.community.feature.config.FeatureConfig;
import java.util.concurrent.CompletableFuture;

/** A Feature is something can be enabled/disabled, may contain commands, and a config */
public interface Feature {

  boolean isEnabled();

  void setEnabled(boolean on);

  void enable();

  void disable();

  FeatureConfig getConfig();

  default CompletableFuture<Integer> count() {
    return CompletableFuture.completedFuture(0);
  }
}

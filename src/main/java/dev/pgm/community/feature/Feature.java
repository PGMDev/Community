package dev.pgm.community.feature;

import dev.pgm.community.CommunityCommand;
import dev.pgm.community.feature.config.FeatureConfig;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/** A Feature is something can be enabled/disabled, may contain commands, and a config */
public interface Feature {

  boolean isEnabled();

  void setEnabled(boolean on);

  void enable();

  void disable();

  Set<CommunityCommand> getCommands();

  FeatureConfig getConfig();

  default CompletableFuture<Integer> count() {
    return CompletableFuture.completedFuture(0);
  }
}

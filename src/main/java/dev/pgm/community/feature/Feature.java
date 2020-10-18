package dev.pgm.community.feature;

import dev.pgm.community.CommunityCommand;
import dev.pgm.community.feature.config.FeatureConfig;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

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

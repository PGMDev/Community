package dev.pgm.community.feature;

import dev.pgm.community.Community;
import dev.pgm.community.feature.config.FeatureConfig;
import java.util.logging.Logger;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

/** A base implementation of a {@link Feature} - Can enable/disable listeners */
public abstract class FeatureBase implements Feature, Listener {

  protected final Logger logger;
  private final FeatureConfig config;
  private final String featureName;

  public FeatureBase(FeatureConfig config, Logger logger, String featureName) {
    this.config = config;
    this.logger = logger;
    this.featureName = featureName;
  }

  @Override
  public void enable() {
    Community.get().registerListener(this);
    logger.info(featureName + " has been enabled");
  }

  @Override
  public void disable() {
    HandlerList.unregisterAll(this);
    logger.info(featureName + " has been disabled");
  }

  @Override
  public boolean isEnabled() {
    return config.isEnabled();
  }

  @Override
  public void setEnabled(boolean yes) {
    config.setEnabled(yes);
  }

  @Override
  public FeatureConfig getConfig() {
    return config;
  }
}

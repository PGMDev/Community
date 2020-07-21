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

  public FeatureBase(FeatureConfig config, Logger logger) {
    this.config = config;
    this.logger = logger;
  }

  @Override
  public void enable() {
    Community.get().registerListener(this);
  }

  @Override
  public void disable() {
    HandlerList.unregisterAll(this);
  }

  @Override
  public boolean isEnabled() {
    return config.isEnabled();
  }

  @Override
  public void setEnabled(boolean yes) {
    this.setEnabled(yes);
  }

  @Override
  public FeatureConfig getConfig() {
    return config;
  }
}

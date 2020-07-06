package dev.pgm.community.feature;

import dev.pgm.community.Community;
import dev.pgm.community.feature.config.FeatureConfig;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

/** A base implementation of a {@link Feature} - Can enable/disable listeners */
public abstract class FeatureBase implements Feature, Listener {

  private FeatureConfig config;

  public FeatureBase(FeatureConfig config) {
    this.config = config;
  }

  public void enable() {
    Community.get().registerListener(this);
  }

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

package dev.pgm.community.broadcast;

import dev.pgm.community.feature.config.FeatureConfigImpl;
import org.bukkit.configuration.Configuration;

public class BroadcastConfig extends FeatureConfigImpl {

  private static final String KEY = "broadcast";

  private String prefix;
  private boolean sound;
  private int titleSeconds;

  public BroadcastConfig(Configuration config) {
    super(KEY, config);
  }

  public String getPrefix() {
    return prefix;
  }

  public boolean isSoundEnabled() {
    return sound;
  }

  public int getTitleSeconds() {
    return titleSeconds;
  }

  @Override
  public void reload(Configuration config) {
    super.reload(config);
    this.prefix = config.getString(getKey() + ".prefix");
    this.sound = config.getBoolean(getKey() + ".sound");
    this.titleSeconds = config.getInt(getKey() + ".title-seconds");
  }
}

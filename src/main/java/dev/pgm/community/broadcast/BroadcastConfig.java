package dev.pgm.community.broadcast;

import dev.pgm.community.feature.config.FeatureConfigImpl;
import java.util.List;
import org.bukkit.configuration.Configuration;

public class BroadcastConfig extends FeatureConfigImpl {

  private static final String KEY = "broadcast";
  private static final String SUB_KEY = "announcements";

  private String prefix;
  private boolean sound;
  private int titleSeconds;

  private boolean announceEnabled;
  private int announceDelay;
  private String announcePrefix;
  private List<String> announceMessages;

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

  public boolean isAnnounceEnabled() {
    return announceEnabled;
  }

  public int getAnnounceDelay() {
    return announceDelay;
  }

  public String getAnnouncePrefix() {
    return announcePrefix;
  }

  public List<String> getAnnounceMessages() {
    return announceMessages;
  }

  private String getAnnounceKey() {
    return getKey() + "." + SUB_KEY;
  }

  @Override
  public void reload(Configuration config) {
    super.reload(config);
    this.prefix = config.getString(getKey() + ".prefix");
    this.sound = config.getBoolean(getKey() + ".sound");
    this.titleSeconds = config.getInt(getKey() + ".title-seconds");

    this.announceEnabled = config.getBoolean(getAnnounceKey() + ".enabled");
    this.announceDelay = config.getInt(getAnnounceKey() + ".delay-seconds");
    this.announcePrefix = config.getString(getAnnounceKey() + ".prefix");
    this.announceMessages = config.getStringList(getAnnounceKey() + ".messages");
  }
}

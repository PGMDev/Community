package dev.pgm.community.broadcast;

import static tc.oc.pgm.util.text.TextParser.parseComponent;

import dev.pgm.community.feature.config.FeatureConfigImpl;
import dev.pgm.community.utils.MessageUtils;
import java.util.List;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.Configuration;

public class BroadcastConfig extends FeatureConfigImpl {

  private static final String KEY = "broadcast";
  private static final String SUB_KEY = "announcements";

  private String prefix;
  private boolean sound;
  private int titleSeconds;

  private boolean announceEnabled;
  private int announceDelay;
  private Component announcePrefix;
  private List<Component> announceMessages;

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

  public Component getAnnouncePrefix() {
    return announcePrefix;
  }

  public List<Component> getAnnounceMessages() {
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
    this.announcePrefix = parseComponent(config.getString(getAnnounceKey() + ".prefix"));
    this.announceMessages = config.getStringList(getAnnounceKey() + ".messages").stream()
        .map(MessageUtils::parseComponentWithURL)
        .collect(Collectors.toList());
  }
}

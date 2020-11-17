package dev.pgm.community.moderation;

import dev.pgm.community.feature.config.FeatureConfigImpl;
import org.bukkit.configuration.Configuration;
import tc.oc.pgm.util.bukkit.BukkitUtils;

public class ModerationConfig extends FeatureConfigImpl {

  // Config Section Keys
  private static final String KEY = "moderation";
  private static final String PERSIST_KEY = ".persist";
  private static final String BROADCAST_KEY = ".broadcast";

  private static final String KICK_KEY = KEY + ".kick";
  private static final String WARN_KEY = KEY + ".warn";
  private static final String BAN_KEY = KEY + ".ban";
  private static final String MUTE_KEY = KEY + ".mute";

  private static final String RULES_KEY = KEY + ".rules-link";
  private static final String APPEAL_KEY = KEY + ".appeal-link";

  private static final String SERVICE_KEY = KEY + ".service";

  // General options
  private boolean persist;
  private boolean broadcast;

  // Punishments
  private boolean kick;
  private boolean warn;
  private boolean ban;
  private boolean mute;

  // Messages
  private String rulesLink;
  private String appealMessage;

  // Service
  private String service;

  /**
   * Config options related to {@link ModerationFeature}
   *
   * @param config {@link Configuration}
   */
  public ModerationConfig(Configuration config) {
    super(KEY, config);
  }

  /**
   * Whether punishments should be saved to the database
   *
   * @return True if should save punishments
   */
  public boolean isPersistent() {
    return persist;
  }

  /**
   * Whether punishments are broadcast to global chat
   *
   * @return True if should broadcast to chat
   */
  public boolean isBroadcasted() {
    return broadcast;
  }

  /**
   * Whether /kick is enabled
   *
   * @return True if kick is enabled
   */
  public boolean isKickEnabled() {
    return kick;
  }

  /**
   * Whether /warn is enabled
   *
   * @return True if warn is enabled
   */
  public boolean isWarnEnabled() {
    return warn;
  }

  /**
   * Whether ban commands are enabled (/ban /tempban)
   *
   * @return True if ban commands are enabled
   */
  public boolean isBanEnabled() {
    return ban;
  }

  /**
   * Whether mute commands are enabled (/mute)
   *
   * @return True if mute commands are enabled
   */
  public boolean isMuteEnabled() {
    return mute;
  }

  /**
   * Get a rules URL to display on kick screen
   *
   * @return a rules URL string
   */
  public String getRulesLink() {
    return rulesLink;
  }

  /**
   * Get a message displayed on kick screen when punishment is permanent
   *
   * @return Appeal message for kick screen
   */
  public String getAppealMessage() {
    return appealMessage != null ? BukkitUtils.colorize(appealMessage) : "";
  }

  public String getService() {
    return service;
  }

  @Override
  public void reload(Configuration config) {
    super.reload(config);
    this.persist = config.getBoolean(PERSIST_KEY, true);
    this.broadcast = config.getBoolean(BROADCAST_KEY, true);

    // Punishment types (kick, warn, ban, mute)
    this.kick = config.getBoolean(getEnabledKey(KICK_KEY));
    this.warn = config.getBoolean(getEnabledKey(WARN_KEY));
    this.ban = config.getBoolean(getEnabledKey(BAN_KEY));
    this.mute = config.getBoolean(getEnabledKey(MUTE_KEY));

    // Messages
    this.rulesLink = config.getString(RULES_KEY);
    this.appealMessage = config.getString(APPEAL_KEY);

    // Service
    this.service = config.getString(SERVICE_KEY);
  }
}

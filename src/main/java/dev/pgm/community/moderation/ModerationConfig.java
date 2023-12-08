package dev.pgm.community.moderation;

import static tc.oc.pgm.util.text.TextParser.parseDuration;

import dev.pgm.community.feature.config.FeatureConfigImpl;
import dev.pgm.community.moderation.feature.ModerationFeature;
import dev.pgm.community.moderation.punishments.Punishment;
import java.time.Duration;
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

  private static final String GLOBAL_FORMAT_KEY = KEY + ".global-broadcast";
  private static final String STAFF_FORMAT_KEY = KEY + ".staff-broadcast";

  private static final String RULES_KEY = KEY + ".rules-link";
  private static final String APPEAL_KEY = KEY + ".appeal-link";

  private static final String SERVICE_KEY = KEY + ".service";

  private static final String TIMEOUT_KEY = KEY + ".login-timeout";

  private static final String MATCH_BAN_KEY = KICK_KEY + ".match-ban";

  private static final String EVASION_MINS = BAN_KEY + ".evasion-expires";

  private static final String STAFF_SIGNOFF_KEY = KEY + ".staff-signoff";

  private static final String TOOLS_KEY = KEY + ".tools";
  private static final String MOD_MENU_KEY = TOOLS_KEY + ".mod-menu";
  private static final String PLAYER_HOOK_KEY = TOOLS_KEY + ".player-hook";
  private static final String LOOKUP_SIGN_KEY = TOOLS_KEY + ".lookup-sign";

  // General options
  private boolean persist;
  private boolean broadcast;

  // Punishments
  private boolean kick;
  private boolean warn;
  private boolean ban;
  private boolean mute;

  private boolean kickPublic;
  private boolean warnPublic;
  private boolean banPublic;
  private boolean mutePublic;

  // Broadcasts
  private String globalBroadcastFormat;
  private String staffBroadcastFormat;

  // Messages
  private String rulesLink;
  private String appealMessage;
  private boolean includeStaffSignoff;

  // Service
  private String service;

  // Login
  private int loginTimeout;

  // Punishment Options

  // 1. Kicks
  private Duration matchBanDuration;

  // 2. Bans
  private int evasionMins;

  // Tools
  private boolean modMenuEnabled;
  private boolean playerHookEnabled;
  private boolean lookupSignEnabled;
  private int modMenuSlot;
  private int playerHookSlot;
  private int lookupSignSlot;

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
   * Get the broadcast visibility of a punishment based on the type
   *
   * @param punishment - a punishment
   * @return True if punishment should be visible publicly
   */
  public boolean isPunishmentPublic(Punishment punishment) {
    switch (punishment.getType()) {
      case TEMP_BAN:
      case BAN:
        return banPublic;
      case KICK:
        return kickPublic;
      case MUTE:
        return mutePublic;
      case WARN:
        return warnPublic;
      default:
        return broadcast;
    }
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

  public int getLoginTimeout() {
    return Math.max(1, loginTimeout);
  }

  public Duration getMatchBanDuration() {
    return matchBanDuration;
  }

  private String getBroadcastKey(String type) {
    return type + ".public";
  }

  public int getEvasionExpireMins() {
    return evasionMins;
  }

  public boolean isStaffSignoff() {
    return includeStaffSignoff;
  }

  public boolean isModMenuEnabled() {
    return modMenuEnabled;
  }

  public boolean isPlayerHookEnabled() {
    return playerHookEnabled;
  }

  public boolean isLookupSignEnabled() {
    return lookupSignEnabled;
  }

  public String getItemSlotKey(String key) {
    return key + ".item-slot";
  }

  public int getModMenuSlot() {
    return modMenuSlot;
  }

  public int getPlayerHookSlot() {
    return playerHookSlot;
  }

  public int getLookupSignSlot() {
    return lookupSignSlot;
  }

  public String getGlobalBroadcastFormat() {
    return globalBroadcastFormat;
  }

  public String getStaffBroadcastFormat() {
    return staffBroadcastFormat;
  }

  @Override
  public void reload(Configuration config) {
    super.reload(config);
    this.persist = config.getBoolean(PERSIST_KEY, true);
    this.broadcast = config.getBoolean(BROADCAST_KEY, true);

    // Punishment options (kick, warn, ban, mute)
    this.kick = config.getBoolean(getEnabledKey(KICK_KEY));
    this.warn = config.getBoolean(getEnabledKey(WARN_KEY));
    this.ban = config.getBoolean(getEnabledKey(BAN_KEY));
    this.mute = config.getBoolean(getEnabledKey(MUTE_KEY));

    this.kickPublic = config.getBoolean(getBroadcastKey(KICK_KEY));
    this.warnPublic = config.getBoolean(getBroadcastKey(WARN_KEY));
    this.mutePublic = config.getBoolean(getBroadcastKey(MUTE_KEY));
    this.banPublic = config.getBoolean(getBroadcastKey(BAN_KEY));

    // Broadcasts
    this.globalBroadcastFormat = config.getString(GLOBAL_FORMAT_KEY);
    this.staffBroadcastFormat = config.getString(STAFF_FORMAT_KEY);

    // Messages
    this.rulesLink = config.getString(RULES_KEY);
    this.appealMessage = config.getString(APPEAL_KEY);
    this.includeStaffSignoff = config.getBoolean(STAFF_SIGNOFF_KEY);

    // Service
    this.service = config.getString(SERVICE_KEY);

    // Logins
    this.loginTimeout = config.getInt(TIMEOUT_KEY);

    // Kicks - match ban
    this.matchBanDuration = parseDuration(config.getString(MATCH_BAN_KEY, "-1"));
    if (matchBanDuration != null && matchBanDuration.isNegative()) {
      matchBanDuration = null;
    }

    // Bans
    this.evasionMins = config.getInt(EVASION_MINS);

    // Tools
    this.modMenuEnabled = config.getBoolean(getEnabledKey(MOD_MENU_KEY));
    this.playerHookEnabled = config.getBoolean(getEnabledKey(PLAYER_HOOK_KEY));
    this.lookupSignEnabled = config.getBoolean(getEnabledKey(LOOKUP_SIGN_KEY));
    this.modMenuSlot = config.getInt(getItemSlotKey(MOD_MENU_KEY));
    this.playerHookSlot = config.getInt(getItemSlotKey(PLAYER_HOOK_KEY));
    this.lookupSignSlot = config.getInt(getItemSlotKey(LOOKUP_SIGN_KEY));
  }
}

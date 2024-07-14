package dev.pgm.community.requests;

import static tc.oc.pgm.util.text.TextParser.parseDuration;

import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.feature.config.FeatureConfigImpl;
import java.time.Duration;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;

public class RequestConfig extends FeatureConfigImpl {

  public static final String KEY = "requests";

  private static final String COOLDOWN = KEY + ".cooldown";

  private static final String SPONSORS = KEY + ".sponsors";
  private static final String SPONSORS_ENABLED = SPONSORS + ".enabled";
  private static final String SPONSORS_COOLDOWN = SPONSORS + ".cooldown";
  private static final String SPONSORS_LIMIT = SPONSORS + ".limit";
  private static final String DAILY_TOKENS = SPONSORS + ".daily-tokens";
  private static final String WEEKLY_TOKENS = SPONSORS + ".weekly-tokens";
  private static final String MAX_TOKENS = SPONSORS + ".max-tokens";
  private static final String REFUND = SPONSORS + ".refund";
  private static final String MAP_COOLDOWN_MULTIPLY = SPONSORS + ".map-cooldown";
  private static final String SCALE_FACTOR = SPONSORS + ".scale-factor";
  private static final String LOWER_LIMIT_OFFSET = SPONSORS + ".lower-limit-offset";
  private static final String UPPER_LIMIT_OFFSET = SPONSORS + ".upper-limit-offset";
  private static final String SUPER_VOTE = KEY + ".super-votes";
  private static final String SUPER_VOTE_ENABLED = SUPER_VOTE + ".enabled";
  private static final String SUPER_VOTE_MULTIPLIER = SUPER_VOTE + ".multiplier";
  private static final String SUPER_VOTE_BROADCAST = SUPER_VOTE + ".broadcast";

  private Duration cooldown; // Cooldown for using /request
  private Duration sponsorCooldown; // Default cooldown for sponsor requests

  private boolean sponsorEnabled; // If sponsor is enabled
  private boolean superVoteEnabled; // If super votes are enabled

  private int dailyTokens; // Amount of tokens given on a daily basis
  private int weeklyTokens; // Amount of tokens given on a weekly basis

  private int maxTokens; // Max amount of tokens a player can earn via daily/weekly refresh

  private int maxQueue; // Max number of requests allowed in sponsor queue

  private boolean refund; // If token should be refunded when vote is successful

  private int mapCooldownMultiply; // # to multiply match length by to determine cooldown

  private double scaleFactor; // Scaling factor for adjusting the upper bound of map size selection

  private int lowerLimitOffset; // Offset to apply on match end to lower map size bound
  private int upperLimitOffset; // Offset to apply on match end to upper map size bound

  private int superVoteMultiplier; // The value a super vote should add
  private boolean superVoteBroadcast; // If super vote activation should be broadcasted

  public RequestConfig(Configuration config) {
    super(KEY, config);
  }

  public Duration getCooldown() {
    return cooldown;
  }

  public boolean isSponsorEnabled() {
    return sponsorEnabled;
  }

  public Duration getSponsorCooldown(Player player) {
    // Check permissions for custom cooldown
    int customHours = -1;
    for (int i = 10; i > 0; i--) {
      if (player.hasPermission(CommunityPermissions.SPONSOR_COOLDOWN_CUSTOM + i)) {
        customHours = i;
      }
    }

    if (customHours == -1) {
      return sponsorCooldown; // Default cooldown
    }

    return Duration.ofHours(customHours); // Custom cooldown
  }

  public int getDailyTokenAmount() {
    return dailyTokens;
  }

  public int getWeeklyTokenAmount() {
    return weeklyTokens;
  }

  public int getMaxTokens() {
    return maxTokens;
  }

  public int getMaxQueue() {
    return maxQueue;
  }

  public boolean isRefunded() {
    return refund;
  }

  public int getMapCooldownMultiply() {
    return mapCooldownMultiply;
  }

  public int getLowerLimitOffset() {
    return lowerLimitOffset;
  }

  public int getUpperLimitOffset() {
    return upperLimitOffset;
  }

  public boolean isSuperVoteEnabled() {
    return superVoteEnabled;
  }

  public int getSuperVoteMultiplier() {
    return superVoteMultiplier;
  }

  public boolean isSuperVoteBroadcast() {
    return superVoteBroadcast;
  }

  public double getScaleFactor() {
    return scaleFactor;
  }

  @Override
  public void reload(Configuration config) {
    super.reload(config);
    this.cooldown = parseDuration(config.getString(COOLDOWN, "15s"));
    this.sponsorEnabled = config.getBoolean(SPONSORS_ENABLED);
    this.sponsorCooldown = parseDuration(config.getString(SPONSORS_COOLDOWN, "1h"));
    this.dailyTokens = config.getInt(DAILY_TOKENS);
    this.weeklyTokens = config.getInt(WEEKLY_TOKENS);
    this.maxTokens = config.getInt(MAX_TOKENS);
    this.maxQueue = config.getInt(SPONSORS_LIMIT);
    this.refund = config.getBoolean(REFUND);
    this.mapCooldownMultiply = config.getInt(MAP_COOLDOWN_MULTIPLY);
    this.scaleFactor = config.getDouble(SCALE_FACTOR);
    this.lowerLimitOffset = config.getInt(LOWER_LIMIT_OFFSET);
    this.upperLimitOffset = config.getInt(UPPER_LIMIT_OFFSET);
    this.superVoteEnabled = config.getBoolean(SUPER_VOTE_ENABLED);
    this.superVoteMultiplier = config.getInt(SUPER_VOTE_MULTIPLIER);
    this.superVoteBroadcast = config.getBoolean(SUPER_VOTE_BROADCAST);
  }
}

package dev.pgm.community.requests;

import static tc.oc.pgm.util.text.TextParser.parseDuration;

import dev.pgm.community.feature.config.FeatureConfigImpl;
import java.time.Duration;
import org.bukkit.configuration.Configuration;

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

  private Duration cooldown; // Cooldown for using /request
  private Duration sponsorCooldown; // Cooldown for sponsor requests

  private boolean sponsors; // If sponsor is enabled

  private int dailyTokens; // Amount of tokens given on a daily basis
  private int weeklyTokens; // Amount of tokens given on a weekly basis

  private int maxTokens; // Max amount of tokens a player can earn via daily/weekly refresh

  private int maxQueue; // Max number of requests allowed in sponsor queue

  public RequestConfig(Configuration config) {
    super(KEY, config);
  }

  public Duration getCooldown() {
    return cooldown;
  }

  public boolean isSponsorEnabled() {
    return sponsors;
  }

  public Duration getSponsorCooldown() {
    return sponsorCooldown;
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

  @Override
  public void reload(Configuration config) {
    super.reload(config);
    this.cooldown = parseDuration(config.getString(COOLDOWN, "15s"));
    this.sponsors = config.getBoolean(SPONSORS_ENABLED);
    this.sponsorCooldown = parseDuration(config.getString(SPONSORS_COOLDOWN, "1h"));
    this.dailyTokens = config.getInt(DAILY_TOKENS);
    this.weeklyTokens = config.getInt(WEEKLY_TOKENS);
    this.maxTokens = config.getInt(MAX_TOKENS);
    this.maxQueue = config.getInt(SPONSORS_LIMIT);
  }
}

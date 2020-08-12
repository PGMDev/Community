package dev.pgm.community.reports;

import dev.pgm.community.feature.config.FeatureConfigImpl;
import org.bukkit.configuration.Configuration;

public class ReportConfig extends FeatureConfigImpl {

  public static final String KEY = "reports";

  private static final String PERSIST_KEY = KEY + ".persist";
  private static final String COOLDOWN_KEY = KEY + ".cooldown";

  private boolean persist;
  private int cooldown;

  /**
   * Configuration options related to reports
   *
   * @param config
   */
  public ReportConfig(Configuration config) {
    super(KEY, config);
  }

  /**
   * Whether reports should be saved to the database
   *
   * @return True if should save reports
   */
  public boolean isPersistent() {
    return persist;
  }

  /**
   * The cooldown in seconds before a player can report again
   *
   * @return cooldown seconds
   */
  public int getCooldown() {
    return cooldown;
  }

  @Override
  public void reload() {
    super.reload();
    this.persist = config.getBoolean(PERSIST_KEY, true);
    this.cooldown = config.getInt(COOLDOWN_KEY, 15);
  }
}

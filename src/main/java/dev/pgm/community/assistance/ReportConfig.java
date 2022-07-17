package dev.pgm.community.assistance;

import static tc.oc.pgm.util.text.TextParser.parseDuration;

import com.google.common.collect.Lists;
import dev.pgm.community.assistance.menu.ReportCategory;
import dev.pgm.community.assistance.menu.ReportReason;
import dev.pgm.community.feature.config.FeatureConfigImpl;
import java.time.Duration;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;

public class ReportConfig extends FeatureConfigImpl {

  public static final String KEY = "reports";
  private static final String PERSIST_KEY = KEY + ".persist";
  private static final String COOLDOWN_KEY = KEY + ".cooldown";
  private static final String MENU_KEY = KEY + ".menu";
  private static final String ALLOW_KEY = KEY + ".allow-input";
  private static final String NOTIFY_SENDER_KEY = KEY + ".notify-sender";
  private static final String REPORT_EXPIRE_KEY = KEY + ".report-expire";
  private static final String NOTIFY_EXPIRE_KEY = KEY + ".notify-expire";

  private static final String CATEGORIES_KEY = KEY + ".categories";

  private static final Material DEFAULT_CATEGORY_ICON = Material.BEACON;
  private static final Material DEFAULT_REASON_ICON = Material.BOOK;

  private boolean persist;
  private boolean menu;
  private boolean allowInput;
  private int cooldown;
  private boolean notifySenders;

  private Duration reportExpireTime;
  private Duration reportNotifyTime;

  private List<ReportCategory> categories;

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

  /**
   * Whether the menu GUI is enabled or disabled.
   *
   * <p>When disabled, reports will use the traditional input `/report [username] (reason)`
   *
   * @return True if menu GUI is enabled
   */
  public boolean isMenu() {
    return menu;
  }

  /**
   * Whether custom input should always be accepted
   *
   * <p>This only has an impact when `menu` is true
   *
   * @return True if custom input is allowed
   */
  public boolean isInputAllowed() {
    return allowInput;
  }

  /**
   * A list of {@link ReportCategory} used for the GUI
   *
   * @return a list of categories
   */
  public List<ReportCategory> getCategories() {
    return categories;
  }

  /**
   * Get whether report senders will be notified upon reported player punishment.
   *
   * @return True if senders are notified
   */
  public boolean isSenderNotified() {
    return notifySenders;
  }

  /**
   * Get how long to cache Reports for.
   *
   * @return A duration of time
   */
  public Duration getReportExpireTime() {
    return reportExpireTime;
  }

  /**
   * Get cutoff period for how long report notifications will be available.
   *
   * @return A duration of time
   */
  public Duration getReporyNotifyTime() {
    return reportNotifyTime;
  }

  @Override
  public void reload(Configuration config) {
    super.reload(config);
    this.persist = config.getBoolean(PERSIST_KEY, true);
    this.cooldown = config.getInt(COOLDOWN_KEY, 15);
    this.menu = config.getBoolean(MENU_KEY, true);
    this.allowInput = config.getBoolean(ALLOW_KEY, true);
    this.notifySenders = config.getBoolean(NOTIFY_SENDER_KEY, true);
    this.reportExpireTime = parseDuration(config.getString(REPORT_EXPIRE_KEY));
    this.reportNotifyTime = parseDuration(config.getString(NOTIFY_EXPIRE_KEY));

    this.categories = Lists.newArrayList();
    ConfigurationSection categories = config.getConfigurationSection(CATEGORIES_KEY);
    if (categories != null) {
      for (String category : categories.getKeys(false)) {
        String name = config.getString(getNameKey(category));
        String icon = config.getString(getIconKey(category));
        List<String> desc = config.getStringList(getDescriptionKey(category));
        List<ReportReason> reasons = Lists.newArrayList();
        ConfigurationSection reasonSection = config.getConfigurationSection(getReasonKey(category));
        if (reasonSection != null) {
          for (String reason : reasonSection.getKeys(false)) {
            String rName = config.getString(getNameKey(formatReasonKey(category, reason)));
            String rIcon = config.getString(getIconKey(formatReasonKey(category, reason)));
            List<String> rDesc =
                config.getStringList(getDescriptionKey(formatReasonKey(category, reason)));
            reasons.add(new ReportReason(rName, rDesc, getMaterial(rIcon, false)));
          }
        }
        this.categories.add(new ReportCategory(name, desc, getMaterial(icon, true), reasons));
      }
    }
  }

  private Material getMaterial(String icon, boolean category) {
    Material material = category ? DEFAULT_CATEGORY_ICON : DEFAULT_REASON_ICON;
    if (icon != null && Material.matchMaterial(icon) != null) {
      material = Material.matchMaterial(icon);
    }
    return material;
  }

  private String getNameKey(String key) {
    return CATEGORIES_KEY + "." + key + ".name";
  }

  private String getIconKey(String key) {
    return CATEGORIES_KEY + "." + key + ".icon";
  }

  private String getDescriptionKey(String key) {
    return CATEGORIES_KEY + "." + key + ".description";
  }

  private String getReasonKey(String key) {
    return CATEGORIES_KEY + "." + key + ".reasons";
  }

  private String formatReasonKey(String category, String reason) {
    return category + ".reasons." + reason;
  }
}

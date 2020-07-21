package dev.pgm.community.reports.feature;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import dev.pgm.community.Community;
import dev.pgm.community.events.PlayerReportEvent;
import dev.pgm.community.feature.FeatureBase;
import dev.pgm.community.reports.Report;
import dev.pgm.community.reports.ReportConfig;
import dev.pgm.community.usernames.UsernameService;
import dev.pgm.community.utils.BroadcastUtils;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import tc.oc.pgm.util.chat.Sound;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.types.PlayerComponent;

public abstract class ReportFeatureBase extends FeatureBase implements ReportFeature {

  private static final Sound REPORT_SOUND =
      new Sound("random.pop", 1f, 1.2f); // Sound played during report broadcast

  private static final int EXPIRES_AFTER = 1;
  private static final TimeUnit RECENT_TIME_UNIT = TimeUnit.HOURS;

  protected final Cache<UUID, Instant> reportCooldown;
  protected final UsernameService usernames;

  protected final Cache<Report, Instant> recentReports;

  public ReportFeatureBase(ReportConfig config, Logger logger, UsernameService usernames) {
    super(config, logger);
    this.usernames = usernames;
    reportCooldown =
        CacheBuilder.newBuilder().expireAfterWrite(config.getCooldown(), TimeUnit.SECONDS).build();
    this.recentReports =
        CacheBuilder.newBuilder().expireAfterWrite(EXPIRES_AFTER, RECENT_TIME_UNIT).build();

    if (config.isEnabled()) {
      enable();
    }
  }

  protected ReportConfig getReportConfig() {
    return (ReportConfig) getConfig();
  }

  private boolean isCooldownEnabled() {
    return getReportConfig().getCooldown() > 0;
  }

  protected boolean isPersistent() {
    return getReportConfig().isPersistent();
  }

  @Override
  public boolean canReport(UUID uuid) {
    return isCooldownEnabled() ? reportCooldown.getIfPresent(uuid) == null : true;
  }

  @Override
  public int getCooldownSeconds(UUID uuid) {
    int seconds = 0;
    Instant lastReport = reportCooldown.getIfPresent(uuid);
    if (lastReport != null) {
      Duration timeElasped = Duration.between(lastReport, Instant.now());
      seconds = Math.toIntExact(getReportConfig().getCooldown() - timeElasped.getSeconds());
    }
    return seconds;
  }

  @Override
  public Report report(Player sender, Player target, String reason) {
    // Create Report
    Report report = new Report(target.getUniqueId(), sender.getUniqueId(), reason, Instant.now());

    // Call Event
    Bukkit.getPluginManager().callEvent(new PlayerReportEvent(report));

    // Reset cooldown
    reportCooldown.put(sender.getUniqueId(), Instant.now());

    return report;
  }

  @Override
  public Set<Report> getRecentReports() {
    return recentReports.asMap().keySet();
  }

  @EventHandler
  public void onPlayerReport(PlayerReportEvent event) {
    recentReports.put(event.getReport(), Instant.now());
    BroadcastUtils.sendAdminChat(formatReportBroadcast(event.getReport()), REPORT_SOUND);
  }

  private Component formatReportBroadcast(Report report) {
    Community.log(report.toString() + " -- EXTRA REASON = %s", report.getReason());
    return TranslatableComponent.of(
        "moderation.report.notify",
        TextColor.GRAY,
        PlayerComponent.of(report.getReporterId(), NameStyle.FANCY),
        PlayerComponent.of(report.getReportedId(), NameStyle.FANCY),
        TextComponent.of(report.getReason(), TextColor.WHITE));
  }
}

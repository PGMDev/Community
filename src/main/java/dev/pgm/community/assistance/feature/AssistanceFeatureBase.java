package dev.pgm.community.assistance.feature;

import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Sets;
import dev.pgm.community.Community;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.assistance.PlayerHelpRequest;
import dev.pgm.community.assistance.Report;
import dev.pgm.community.assistance.ReportConfig;
import dev.pgm.community.assistance.commands.PlayerHelpCommand;
import dev.pgm.community.assistance.commands.ReportCommands;
import dev.pgm.community.events.PlayerHelpRequestEvent;
import dev.pgm.community.events.PlayerReportEvent;
import dev.pgm.community.feature.FeatureBase;
import dev.pgm.community.utils.BroadcastUtils;
import dev.pgm.community.utils.Sounds;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.PlayerComponent;

public abstract class AssistanceFeatureBase extends FeatureBase implements AssistanceFeature {

  private static final int EXPIRES_AFTER = 1;
  private static final TimeUnit RECENT_TIME_UNIT = TimeUnit.HOURS;

  protected final Cache<UUID, Instant> cooldown;
  protected final Cache<Report, Instant> recentReports;
  protected final Cache<PlayerHelpRequest, Instant> recentHelp;

  public AssistanceFeatureBase(ReportConfig config, Logger logger, String featureName) {
    super(config, logger, featureName);
    cooldown =
        CacheBuilder.newBuilder().expireAfterWrite(config.getCooldown(), TimeUnit.SECONDS).build();
    this.recentReports =
        CacheBuilder.newBuilder().expireAfterWrite(EXPIRES_AFTER, RECENT_TIME_UNIT).build();
    this.recentHelp =
        CacheBuilder.newBuilder().expireAfterWrite(EXPIRES_AFTER, RECENT_TIME_UNIT).build();

    if (config.isEnabled()) {
      enable();
    }
  }

  protected ReportConfig getReportConfig() {
    return (ReportConfig) getConfig();
  }

  @Override
  public Set<CommunityCommand> getCommands() {
    return getReportConfig().isEnabled()
        ? Sets.newHashSet(new ReportCommands(), new PlayerHelpCommand())
        : Sets.newHashSet();
  }

  private boolean isCooldownEnabled() {
    return getReportConfig().getCooldown() > 0;
  }

  protected boolean isPersistent() {
    return getReportConfig().isPersistent();
  }

  @Override
  public boolean canRequest(UUID uuid) {
    return isCooldownEnabled() ? cooldown.getIfPresent(uuid) == null : true;
  }

  @Override
  public int getCooldownSeconds(UUID uuid) {
    int seconds = 0;
    Instant lastReport = cooldown.getIfPresent(uuid);
    if (lastReport != null) {
      Duration timeElasped = Duration.between(lastReport, Instant.now());
      seconds = Math.toIntExact(getReportConfig().getCooldown() - timeElasped.getSeconds());
    }
    return seconds;
  }

  @Override
  public Component getCooldownMessage(UUID playerId) {
    int cooldown = getCooldownSeconds(playerId);
    Component secondsComponent = text(Integer.toString(cooldown));
    Component secondsLeftComponent =
        translatable(
            cooldown != 1 ? "misc.seconds" : "misc.second", NamedTextColor.AQUA, secondsComponent);
    return translatable("command.cooldown", secondsLeftComponent);
  }

  @Override
  public Report report(Player sender, Player target, String reason) {
    // Self reporting results in an assistance message
    if (sender.equals(target)) {
      assist(sender, reason);
      return null;
    }

    // Create Report
    Report report =
        new Report(
            target.getUniqueId(),
            sender.getUniqueId(),
            reason,
            Instant.now(),
            Community.get().getServerConfig().getServerId());

    // Call Event
    Bukkit.getPluginManager().callEvent(new PlayerReportEvent(report));

    // Reset cooldown
    cooldown.put(sender.getUniqueId(), Instant.now());

    return report;
  }

  @Override
  public Set<Report> getRecentReports() {
    return recentReports.asMap().keySet();
  }

  @Override
  public void assist(Player sender, String reason) {
    // Create help request
    PlayerHelpRequest help = new PlayerHelpRequest(sender, reason);

    // Call Event
    Bukkit.getPluginManager().callEvent(new PlayerHelpRequestEvent(help));

    // Send user feedback
    sendHelpRequestFeedback(sender);

    // Reset cooldown
    cooldown.put(sender.getUniqueId(), Instant.now());
  }

  @EventHandler
  public void onPlayerReport(PlayerReportEvent event) {
    recentReports.put(event.getReport(), Instant.now());
    BroadcastUtils.sendAdminChatMessage(
        formatReportBroadcast(event.getReport()), Sounds.PLAYER_REPORT);
  }

  @EventHandler
  public void onPlayerRequestHelp(PlayerHelpRequestEvent event) {
    BroadcastUtils.sendAdminChatMessage(
        formatHelpBroadcast(event.getRequest()), Sounds.HELP_REQUEST);
  }

  private Component formatReportBroadcast(Report report) {
    return translatable(
        "moderation.report.notify",
        NamedTextColor.GRAY,
        PlayerComponent.player(report.getReporterId(), NameStyle.FANCY),
        PlayerComponent.player(report.getReportedId(), NameStyle.FANCY),
        text(report.getReason(), NamedTextColor.WHITE));
  }

  private Component formatHelpBroadcast(PlayerHelpRequest request) {
    return text()
        .append(PlayerComponent.player(request.getPlayerId(), NameStyle.FANCY))
        .append(text(" requires assistance ", NamedTextColor.GRAY)) // TODO: translate
        .append(BroadcastUtils.RIGHT_DIV.color(NamedTextColor.YELLOW))
        .append(space())
        .append(text(request.getReason(), NamedTextColor.WHITE))
        .build();
  }

  private void sendHelpRequestFeedback(Player player) {
    Component thanks =
        text()
            .append(translatable("misc.thankYou", NamedTextColor.GREEN))
            .append(space())
            .append(
                text(
                    "A staff member will assist you once available",
                    NamedTextColor.GOLD)) // TODO: translate
            .hoverEvent(
                HoverEvent.showText(
                    text(
                        "Please note: not all requests can be accommodated. However, we will do our best to help",
                        NamedTextColor.GRAY)))
            .build();
    Audience.get(player).sendMessage(thanks);
  }
}

package dev.pgm.community.assistance.feature;

import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import dev.pgm.community.assistance.AssistanceRequest;
import dev.pgm.community.assistance.AssistanceRequest.RequestType;
import dev.pgm.community.assistance.PlayerHelpRequest;
import dev.pgm.community.assistance.Report;
import dev.pgm.community.assistance.ReportConfig;
import dev.pgm.community.assistance.menu.ReportCategoryMenu;
import dev.pgm.community.events.PlayerHelpRequestEvent;
import dev.pgm.community.events.PlayerPunishmentEvent;
import dev.pgm.community.events.PlayerReportEvent;
import dev.pgm.community.feature.FeatureBase;
import dev.pgm.community.network.feature.NetworkFeature;
import dev.pgm.community.network.subs.types.AssistanceSubscriber;
import dev.pgm.community.network.updates.types.AssistUpdate;
import dev.pgm.community.users.feature.UsersFeature;
import dev.pgm.community.utils.BroadcastUtils;
import dev.pgm.community.utils.NetworkUtils;
import dev.pgm.community.utils.Sounds;
import fr.minuskube.inv.InventoryManager;
import fr.minuskube.inv.SmartInventory;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.bukkit.BukkitUtils;
import tc.oc.pgm.util.named.NameStyle;

public abstract class AssistanceFeatureBase extends FeatureBase implements AssistanceFeature {

  private final NetworkFeature network;
  protected final UsersFeature users;
  private final InventoryManager inventory;

  protected final Cache<UUID, Instant> cooldown;
  protected final Cache<Report, Instant> recentReports;
  protected final Cache<PlayerHelpRequest, Instant> recentHelp;

  public AssistanceFeatureBase(
      ReportConfig config,
      Logger logger,
      String featureName,
      NetworkFeature network,
      UsersFeature users,
      InventoryManager inventory) {
    super(config, logger, featureName);
    cooldown =
        CacheBuilder.newBuilder().expireAfterWrite(config.getCooldown(), TimeUnit.SECONDS).build();
    this.recentReports =
        CacheBuilder.newBuilder()
            .expireAfterWrite(config.getReportExpireTime().getSeconds(), TimeUnit.SECONDS)
            .build();
    this.recentHelp =
        CacheBuilder.newBuilder()
            .expireAfterWrite(config.getReportExpireTime().getSeconds(), TimeUnit.SECONDS)
            .build();
    this.network = network;
    this.users = users;
    this.inventory = inventory;

    if (config.isEnabled()) {
      enable();
      network.registerSubscriber(new AssistanceSubscriber(this, network.getNetworkId(), logger));
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
  public void requestAssistance(Player sender, Player target, @Nullable String reason) {
    if (!getReportConfig().isMenu()
        || sender == target
        || (getReportConfig().isInputAllowed() && reason != null)) {
      report(sender, target, reason);
    } else {
      openReportsMenu(sender, target);
    }
  }

  @Override
  public Report report(Player sender, Player target, String reason) {
    if (reason == null || reason.isEmpty()) {
      Audience.get(sender).sendWarning(text("Please provide a reason"));
      return null;
    }

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
            NetworkUtils.getServer());

    // Call Event
    Bukkit.getPluginManager().callEvent(new PlayerReportEvent(report));

    // Send user feedback
    sendReportFeedback(sender);

    // Reset cooldown
    startCooldown(sender);

    return report;
  }

  @Override
  public Set<Report> getRecentReports() {
    return recentReports.asMap().keySet();
  }

  @Override
  public void assist(Player sender, String reason) {
    // Create help request
    PlayerHelpRequest help = new PlayerHelpRequest(sender, reason, NetworkUtils.getServer());

    // Call Event
    Bukkit.getPluginManager().callEvent(new PlayerHelpRequestEvent(help));

    // Send user feedback
    sendHelpRequestFeedback(sender);

    // Reset cooldown
    startCooldown(sender);
  }

  @Override
  public void sendUpdate(AssistanceRequest request) {
    network.sendUpdate(new AssistUpdate(request));
  }

  @Override
  public void recieveUpdate(AssistanceRequest request) {
    if (request.getType() == RequestType.REPORT) {
      invalidate(request.getTargetId());
    }
    broadcastRequest(request);
  }

  @EventHandler
  public void onPlayerReport(PlayerReportEvent event) {
    recentReports.put(event.getReport(), Instant.now());
    sendUpdate(event.getReport());
    broadcastRequest(event.getReport());
  }

  @EventHandler
  public void onPlayerRequestHelp(PlayerHelpRequestEvent event) {
    recentHelp.put(event.getRequest(), Instant.now());
    sendUpdate(event.getRequest());
    broadcastRequest(event.getRequest());
  }

  private void startCooldown(Player sender) {
    cooldown.put(sender.getUniqueId(), Instant.now());
  }

  private void broadcastRequest(AssistanceRequest request) {
    final String server = request.getServer();
    final String reason = request.getReason();
    final boolean report = request.getType() == RequestType.REPORT;
    CompletableFuture<Component> sender =
        users.renderUsername(request.getSenderId(), NameStyle.FANCY);
    CompletableFuture<Component> target =
        users.renderUsername(request.getTargetId(), NameStyle.FANCY);

    CompletableFuture.allOf(sender, target)
        .thenAcceptAsync(
            x -> {
              Component senderName = sender.join();
              Component targetName = target.join();
              broadcastRequest(server, senderName, targetName, reason, report);
            });
  }

  private void broadcastRequest(
      String server, Component sender, Component target, String reason, boolean report) {
    Component component =
        report
            ? formatReportBroadcast(sender, target, reason)
            : formatHelpBroadcast(sender, reason);
    Sound sound = report ? Sounds.PLAYER_REPORT : Sounds.HELP_REQUEST;
    BroadcastUtils.sendAdminChatMessage(component, server, sound, null);
  }

  private Component formatReportBroadcast(Component sender, Component target, String reason) {
    return translatable(
        "moderation.report.notify",
        NamedTextColor.GRAY,
        sender,
        target,
        text(reason, NamedTextColor.WHITE));
  }

  private Component formatHelpBroadcast(Component sender, String reason) {
    return text()
        .append(sender)
        .append(text(" requires assistance ", NamedTextColor.GRAY)) // TODO: translate
        .append(BroadcastUtils.RIGHT_DIV.color(NamedTextColor.YELLOW))
        .append(space())
        .append(text(reason, NamedTextColor.WHITE))
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

  private void sendReportFeedback(Player player) {
    Component thanks =
        text()
            .append(translatable("misc.thankYou", NamedTextColor.GREEN))
            .append(space())
            .append(translatable("moderation.report.acknowledge", NamedTextColor.GOLD))
            .build();
    Audience.get(player).sendMessage(thanks);
  }

  @Override
  public void openReportsMenu(Player player, Player target) {
    SmartInventory.builder()
        .size(1, 9)
        .title(BukkitUtils.colorize("&eSelect a category&7:"))
        .manager(inventory)
        .provider(
            new ReportCategoryMenu(inventory, target, this, getReportConfig().getCategories()))
        .build()
        .open(player);
  }

  @EventHandler
  public void onPunishment(PlayerPunishmentEvent event) {
    if (!getReportConfig().isSenderNotified()) return;

    List<Report> relatedReports =
        recentReports.asMap().keySet().stream()
            .filter(r -> r.getTargetId().equals(event.getPunishment().getTargetId()))
            .filter(r -> !r.hasNotified())
            .filter(
                r ->
                    !getReportConfig()
                        .getReporyNotifyTime()
                        .minus(Duration.between(r.getTime(), Instant.now()))
                        .isNegative())
            .collect(Collectors.toList());
    Set<UUID> reporters =
        relatedReports.stream().map(r -> r.getSenderId()).collect(Collectors.toSet());
    for (UUID reporterId : reporters) {
      Player onlineReporter = Bukkit.getPlayer(reporterId);
      if (onlineReporter != null) {
        Audience.get(onlineReporter)
            .sendMessage(
                text()
                    .append(
                        text(
                            "A player you recently reported has been punished. ",
                            NamedTextColor.GOLD))
                    .append(text("Thanks for the help!", NamedTextColor.GREEN)));
      }
    }
    for (Report report : relatedReports) {
      report.setNotified(true);
    }
  }
}

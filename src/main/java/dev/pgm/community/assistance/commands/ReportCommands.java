package dev.pgm.community.assistance.commands;

import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.util.text.TextException.exception;

import dev.pgm.community.Community;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.assistance.Report;
import dev.pgm.community.assistance.feature.AssistanceFeature;
import dev.pgm.community.moderation.feature.ModerationFeature;
import dev.pgm.community.moderation.punishments.types.MutePunishment;
import dev.pgm.community.users.feature.UsersFeature;
import dev.pgm.community.utils.CommandAudience;
import dev.pgm.community.utils.PaginatedComponentResults;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import tc.oc.pgm.lib.cloud.commandframework.annotations.Argument;
import tc.oc.pgm.lib.cloud.commandframework.annotations.CommandDescription;
import tc.oc.pgm.lib.cloud.commandframework.annotations.CommandMethod;
import tc.oc.pgm.lib.cloud.commandframework.annotations.CommandPermission;
import tc.oc.pgm.lib.cloud.commandframework.annotations.specifier.Greedy;
import tc.oc.pgm.lib.cloud.commandframework.annotations.specifier.Range;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.player.PlayerComponent;
import tc.oc.pgm.util.text.TemporalComponent;
import tc.oc.pgm.util.text.TextFormatter;

public class ReportCommands extends CommunityCommand {

  private final AssistanceFeature reports;
  private final UsersFeature usernames;
  private final ModerationFeature moderation;

  public ReportCommands() {
    this.reports = Community.get().getFeatures().getReports();
    this.usernames = Community.get().getFeatures().getUsers();
    this.moderation = Community.get().getFeatures().getModeration();
  }

  @CommandMethod("report <username> [reason]")
  @CommandDescription("Report a player who is breaking the rules")
  public void report(
      CommandAudience viewer,
      Player sender,
      @Argument("username") Player target,
      @Argument("reason") @Greedy String reason) {
    checkEnabled();
    Optional<MutePunishment> mute = moderation.getCachedMute(sender.getUniqueId());
    if (mute.isPresent()) {
      viewer.sendWarning(mute.get().getChatMuteMessage());
      return;
    }

    if (!reports.canRequest(sender.getUniqueId())) {
      int cooldown = reports.getCooldownSeconds(sender.getUniqueId());
      if (cooldown > 0) {
        viewer.sendWarning(reports.getCooldownMessage(sender.getUniqueId()));
        return;
      }
    }

    if (target != null) {
      reports.requestAssistance(sender, target, reason);
    }
  }

  @CommandMethod("reports|reporthistory|reps [page]")
  @CommandDescription("View report history")
  @CommandPermission(CommunityPermissions.REPORTS)
  public void recentReports(
      CommandAudience audience,
      @Argument(value = "page", defaultValue = "1") @Range(min = "1") int page) {
    checkEnabled();
    sendReportHistory(audience, reports.getRecentReports(), page);
  }

  @CommandMethod("reports|reporthistory|reps player <player> [page]")
  @CommandDescription("View a list of recent reports for a target player")
  @CommandPermission(CommunityPermissions.REPORTS)
  public void sendPlayerReportHistory(
      final CommandAudience audience,
      @Argument("player") String target,
      @Argument(value = "page", defaultValue = "1") @Range(min = "1") final int page) {
    checkEnabled();
    reports
        .query(target)
        .thenAcceptAsync(
            reports -> {
              if (reports.isEmpty()) {
                audience.sendWarning(
                    text("No reports found for ").append(text(target, NamedTextColor.AQUA)));
                return;
              }
              sendReportHistory(audience, reports, page);
            });
  }

  public void sendReportHistory(CommandAudience audience, Collection<Report> reportData, int page) {
    Component headerResultCount = text(Long.toString(reportData.size()), NamedTextColor.RED);

    int perPage = 7;
    int pages = (reportData.size() + perPage - 1) / perPage;
    page = Math.max(1, Math.min(page, pages));

    Component pageNum =
        translatable(
            "command.simplePageHeader",
            NamedTextColor.GRAY,
            text(Integer.toString(page), NamedTextColor.RED),
            text(Integer.toString(pages), NamedTextColor.RED));

    Component header =
        translatable("moderation.reports.header", NamedTextColor.GRAY, headerResultCount, pageNum)
            .append(text(" (").append(headerResultCount).append(text(") » ")).append(pageNum));

    Component formattedHeader =
        TextFormatter.horizontalLineHeading(audience.getSender(), header, NamedTextColor.DARK_GRAY);
    new PaginatedComponentResults<Report>(formattedHeader, perPage) {
      @Override
      public Component format(Report data, int index) {
        Component reporterName = getReportFormatName(data.getSenderId()).join();
        Component reportedName = getReportFormatName(data.getTargetId()).join();

        Component serverName =
            text("Server ", NamedTextColor.GRAY)
                .append(text(": ", NamedTextColor.DARK_GRAY))
                .append(text(data.getServer(), NamedTextColor.AQUA));

        TextComponent.Builder reporter =
            text()
                .append(
                    translatable("moderation.reports.hover", NamedTextColor.GRAY, reporterName));

        if (!data.getServer().equalsIgnoreCase(Community.get().getServerConfig().getServerId())) {
          reporter.append(newline()).append(serverName);
        }

        Component timeAgo =
            TemporalComponent.relativePastApproximate(data.getTime())
                .color(NamedTextColor.DARK_GREEN);

        return text()
            .append(timeAgo.hoverEvent(HoverEvent.showText(reporter.build())))
            .append(text(": ", NamedTextColor.GRAY))
            .append(reportedName)
            .append(text(" « ", NamedTextColor.YELLOW))
            .append(text(data.getReason(), NamedTextColor.WHITE, TextDecoration.ITALIC))
            .build();
      }

      @Override
      public Component formatEmpty() {
        return text("No reports found", NamedTextColor.RED);
      }
    }.display(
        audience.getAudience(), reportData.stream().sorted().collect(Collectors.toList()), page);
  }

  private CompletableFuture<Component> getReportFormatName(UUID id) {
    return usernames
        .getStoredUsername(id)
        .thenApplyAsync(
            name -> {
              return PlayerComponent.player(Bukkit.getPlayer(id), name, NameStyle.FANCY);
            });
  }

  private void checkEnabled() {
    if (!reports.isEnabled()) {
      throw exception("Reports are not enabled");
    }
  }
}

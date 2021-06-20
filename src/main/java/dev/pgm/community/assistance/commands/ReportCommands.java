package dev.pgm.community.assistance.commands;

import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import dev.pgm.community.Community;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.assistance.Report;
import dev.pgm.community.assistance.feature.AssistanceFeature;
import dev.pgm.community.moderation.feature.ModerationFeature;
import dev.pgm.community.moderation.punishments.types.MutePunishment;
import dev.pgm.community.nick.feature.NickFeature;
import dev.pgm.community.users.feature.UsersFeature;
import dev.pgm.community.utils.CommandAudience;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.PlayerComponent;
import tc.oc.pgm.util.text.TemporalComponent;
import tc.oc.pgm.util.text.TextFormatter;
import tc.oc.pgm.util.text.formatting.PaginatedComponentResults;

public class ReportCommands extends CommunityCommand {

  @Dependency private AssistanceFeature reports;
  @Dependency private UsersFeature usernames;
  @Dependency private NickFeature nick;
  @Dependency private ModerationFeature moderation;

  @CommandAlias("report")
  @Description("Report a player who is breaking the rules")
  @CommandCompletion("@visible *")
  @Syntax("[username] (reason)")
  public void report(
      CommandAudience viewer, Player sender, String target, @Optional String reason) {
    checkEnabled();
    java.util.Optional<MutePunishment> mute = moderation.getCachedMute(sender.getUniqueId());
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

    Player targetPlayer = getSinglePlayer(viewer, target, true);
    if (targetPlayer != null) {
      reports.requestAssistance(sender, targetPlayer, reason);
    }
  }

  // Example of a nested command.
  // Both /reports [page] and /reports player [target] [page] will work

  // Future sub-commands could include time or server searches
  // Ex. /reports server [server] or /reports time 5h
  @CommandAlias("reports|reporthistory|reps")
  @Description("View report history")
  @CommandPermission(CommunityPermissions.REPORTS)
  public class ReportHistory extends CommunityCommand {

    @Subcommand("player|pl")
    @Description("View a list of recent reports for a target player")
    @Syntax("[player] [page]")
    @CommandCompletion("@players")
    // Command completion here is for ALL online, however can provide any
    // username/uuid to lookup past reports from DB
    public void sendPlayerReportHistory(
        final CommandAudience audience, final String target, @Default("1") final int page) {
      checkEnabled();

      // Sub command allows for a player target to be specified
      // TODO: search for player reports in DB, sort by most recent
      reports
          .query(target)
          .thenAcceptAsync(
              reports -> {
                if (reports.isEmpty()) {
                  // TODO: Translate this
                  audience.sendWarning(
                      text("No reports found for ").append(text(target, NamedTextColor.AQUA)));
                  return;
                }
                sendReportHistory(audience, reports, page);
              });
      // Example of command syntax: /reports player applenick (page)
    }

    @Default // Default is a catch-all, /reports will default to this when no input is present
    @Syntax("[page]")
    public void recentReports(CommandAudience audience, @Default("1") int page) {
      checkEnabled();
      sendReportHistory(audience, reports.getRecentReports(), page);
    }

    public void sendReportHistory(
        CommandAudience audience, Collection<Report> reportData, int page) {
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
          TextFormatter.horizontalLineHeading(
              audience.getSender(), header, NamedTextColor.DARK_GRAY);
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
          // TODO: Translate
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
  }

  private void checkEnabled() throws InvalidCommandArgument {
    if (!reports.isEnabled()) {
      throw new InvalidCommandArgument(ChatColor.RED + "Reports are not enabled", false);
    }
  }
}

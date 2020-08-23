package dev.pgm.community.reports.commands;

import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.reports.Report;
import dev.pgm.community.reports.feature.ReportFeature;
import dev.pgm.community.users.feature.UsersFeature;
import dev.pgm.community.utils.CommandAudience;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.event.HoverEvent;
import net.kyori.text.event.HoverEvent.Action;
import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.PeriodFormats;
import tc.oc.pgm.util.text.TextFormatter;
import tc.oc.pgm.util.text.formatting.PaginatedComponentResults;
import tc.oc.pgm.util.text.types.PlayerComponent;

public class ReportCommands extends CommunityCommand {

  @Dependency private ReportFeature reports;
  @Dependency private UsersFeature usernames;

  @CommandAlias("report")
  @Description("Report a player who is breaking the rules")
  @CommandCompletion("@players *")
  @Syntax("[username] (reason)")
  public void report(CommandAudience viewer, Player sender, OnlinePlayer target, String reason) {
    checkEnabled();

    if (!reports.canReport(sender.getUniqueId())) {
      int cooldown = reports.getCooldownSeconds(sender.getUniqueId());
      if (cooldown > 0) {
        TextComponent secondsComponent = TextComponent.of(Integer.toString(cooldown));
        TranslatableComponent secondsLeftComponent =
            TranslatableComponent.of(
                cooldown != 1 ? "misc.seconds" : "misc.second", TextColor.AQUA, secondsComponent);
        viewer.sendWarning(TranslatableComponent.of("command.cooldown", secondsLeftComponent));
        return;
      }
    }

    reports.report(sender, target.getPlayer(), reason);

    Component thanks =
        TextComponent.builder()
            .append(TranslatableComponent.of("misc.thankYou", TextColor.GREEN))
            .append(TextComponent.space())
            .append(TranslatableComponent.of("moderation.report.acknowledge", TextColor.GOLD))
            .build();
    viewer.sendMessage(thanks);
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
                      TextComponent.builder("No reports found for ")
                          .append(target, TextColor.AQUA)
                          .build());
                  return;
                }
                sendReportHistory(audience, reports, page);
              });
      // Example of command syntax: /reports player applenick (page)
    }

    @Default // Default is a catch-all, /reports will default to this when no input is present
    public void recentReports(CommandAudience audience, @Default("1") int page) {
      checkEnabled();
      sendReportHistory(audience, reports.getRecentReports(), page);
    }

    public void sendReportHistory(
        CommandAudience audience, Collection<Report> reportData, int page) {
      Component headerResultCount =
          TextComponent.of(Long.toString(reportData.size()), TextColor.RED);

      int perPage = 7;
      int pages = (reportData.size() + perPage - 1) / perPage;
      page = Math.max(1, Math.min(page, pages));

      Component pageNum =
          TranslatableComponent.of(
              "command.simplePageHeader",
              TextColor.GRAY,
              TextComponent.of(Integer.toString(page), TextColor.RED),
              TextComponent.of(Integer.toString(pages), TextColor.RED));

      Component header =
          TranslatableComponent.of(
                  "moderation.reports.header", TextColor.GRAY, headerResultCount, pageNum)
              .append(
                  TextComponent.of(" (")
                      .append(headerResultCount)
                      .append(TextComponent.of(") » "))
                      .append(pageNum));

      Component formattedHeader =
          TextFormatter.horizontalLineHeading(audience.getSender(), header, TextColor.DARK_GRAY);
      new PaginatedComponentResults<Report>(formattedHeader, perPage) {
        @Override
        public Component format(Report data, int index) {
          Component reporterName = getReportFormatName(data.getReporterId());
          Component reportedName = getReportFormatName(data.getReportedId());

          Component reporter =
              TranslatableComponent.of("moderation.reports.hover", TextColor.GRAY, reporterName);
          Component timeAgo =
              PeriodFormats.relativePastApproximate(data.getTime()).color(TextColor.DARK_GREEN);

          return TextComponent.builder()
              .append(timeAgo.hoverEvent(HoverEvent.of(Action.SHOW_TEXT, reporter)))
              .append(": ", TextColor.GRAY)
              .append(reportedName)
              .append(" « ", TextColor.YELLOW)
              .append(data.getReason(), TextColor.WHITE, TextDecoration.ITALIC)
              .build();
        }

        @Override
        public Component formatEmpty() {
          // TODO: Translate
          return TextComponent.of("No reports found", TextColor.RED);
        }
      }.display(
          audience.getAudience(), reportData.stream().sorted().collect(Collectors.toList()), page);
    }

    private Component getReportFormatName(UUID id) {
      return PlayerComponent.of(Bukkit.getPlayer(id), usernames.getUsername(id), NameStyle.FANCY);
    }
  }

  private void checkEnabled() throws InvalidCommandArgument {
    if (!reports.isEnabled()) {
      throw new InvalidCommandArgument(ChatColor.RED + "Reports are not enabled", false);
    }
  }
}

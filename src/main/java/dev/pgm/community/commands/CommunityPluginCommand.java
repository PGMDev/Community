package dev.pgm.community.commands;

import static net.kyori.adventure.text.Component.text;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import dev.pgm.community.Community;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.feature.Feature;
import dev.pgm.community.moderation.ModerationConfig;
import dev.pgm.community.moderation.feature.ModerationFeature;
import dev.pgm.community.reports.feature.ReportFeature;
import dev.pgm.community.users.feature.UsersFeature;
import dev.pgm.community.utils.CommandAudience;
import dev.pgm.community.utils.ImportUtils;
import dev.pgm.community.utils.ImportUtils.BukkitBanEntry;
import java.util.List;
import net.kyori.adventure.text.format.NamedTextColor;
import tc.oc.pgm.util.UsernameResolver;
import tc.oc.pgm.util.text.TextFormatter;

// TODO: Maybe move to a different place
@CommandAlias("community")
@Description("Manage the community plugin")
@CommandPermission(CommunityPermissions.RELOAD)
public class CommunityPluginCommand extends CommunityCommand {

  @Dependency private Community plugin;
  @Dependency private ModerationFeature moderation;
  @Dependency private UsersFeature users;
  @Dependency private ReportFeature reports;

  @Default
  public void reload(CommandAudience audience) {
    plugin.reload();
    audience.sendWarning(text("Community has been reloaded")); // TODO: translate
  }

  @Subcommand("stats")
  public void stats(CommandAudience audience) {
    audience.sendMessage(
        TextFormatter.horizontalLineHeading(
            audience.getSender(),
            text("Community Database Stats", NamedTextColor.YELLOW),
            NamedTextColor.DARK_RED));
    sendTotalCount(users, "Total Users", audience);
    sendTotalCount(moderation, "Total Punishments", audience);
    sendTotalCount(reports, "Total Reports", audience);
  }

  private void sendTotalCount(Feature feature, String countName, CommandAudience audience) {
    feature
        .count()
        .thenAcceptAsync(
            total ->
                audience.sendMessage(
                    text()
                        .append(text(countName, NamedTextColor.GOLD))
                        .append(text(": ", NamedTextColor.GRAY))
                        .append(text(total, NamedTextColor.GREEN))
                        .build()));
  }

  @Subcommand("punishments|p")
  public class ModerationAdminCommands extends CommunityCommand {
    // TODO: Look into cleaning up messages and redo verbose stuff
    @Subcommand("import")
    @Syntax("[true/false] - Verbose message")
    public void importBans(CommandAudience viewer, @Default("false") boolean verbose) {
      viewer.sendWarning(
          text(
              "Now importing bans to Community database, this may take a while...",
              NamedTextColor.DARK_RED));

      // Get a list of banned players
      List<BukkitBanEntry> bans = ImportUtils.getBukkitBans();
      viewer.sendMessage(
          text("Parsed ")
              .append(text(Integer.toString(bans.size()), NamedTextColor.GREEN))
              .append(text(" bans from "))
              .append(text("banned-players.json", NamedTextColor.AQUA))
              .color(NamedTextColor.GRAY));

      bans.stream()
          .forEach(
              ban -> {
                // Save punishment first
                moderation.save(ban.toPunishment((ModerationConfig) moderation.getConfig(), users));

                // Resolve UUID to fetch latest username & update, since profiles may not exist yet
                UsernameResolver.resolve(
                    ban.getUUID(),
                    name -> {
                      users.saveImportedUser(ban.getUUID(), name);
                      if (verbose) {
                        Community.log(
                            "Resolved %s to %s and saved both to database",
                            ban.getUUID().toString(), name);
                      }
                    });
              });
      // After all are added, start resolving usernames
      UsernameResolver.resolveAll();
    }
  }
}

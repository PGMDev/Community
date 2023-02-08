package dev.pgm.community.commands;

import static net.kyori.adventure.text.Component.text;

import cloud.commandframework.annotations.CommandDescription;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import dev.pgm.community.Community;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.assistance.feature.AssistanceFeature;
import dev.pgm.community.feature.Feature;
import dev.pgm.community.feature.FeatureManager;
import dev.pgm.community.moderation.feature.ModerationFeature;
import dev.pgm.community.users.feature.UsersFeature;
import dev.pgm.community.utils.CommandAudience;
import net.kyori.adventure.text.format.NamedTextColor;
import tc.oc.pgm.util.text.TextFormatter;

public class CommunityPluginCommand extends CommunityCommand {

  private static final String CMD_NAME = "community";

  private final ModerationFeature moderation;
  private final UsersFeature users;
  private final AssistanceFeature reports;

  public CommunityPluginCommand(FeatureManager features) {
    this.moderation = features.getModeration();
    this.users = features.getUsers();
    this.reports = features.getReports();
  }

  @CommandMethod(CMD_NAME)
  @CommandDescription("Reload the plugin")
  @CommandPermission(CommunityPermissions.RELOAD)
  public void reload(CommandAudience audience) {
    Community.get().reload();
    audience.sendWarning(text("Community has been reloaded")); // TODO: translate
  }

  @CommandMethod(CMD_NAME + " stats")
  @CommandDescription("View database stats")
  @CommandPermission(CommunityPermissions.RELOAD)
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
}

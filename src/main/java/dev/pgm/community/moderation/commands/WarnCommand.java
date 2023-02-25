package dev.pgm.community.moderation.commands;

import dev.pgm.community.Community;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.commands.player.TargetPlayer;
import dev.pgm.community.moderation.feature.ModerationFeature;
import dev.pgm.community.moderation.punishments.PunishmentType;
import dev.pgm.community.users.feature.UsersFeature;
import dev.pgm.community.utils.CommandAudience;
import tc.oc.pgm.lib.cloud.commandframework.annotations.Argument;
import tc.oc.pgm.lib.cloud.commandframework.annotations.CommandDescription;
import tc.oc.pgm.lib.cloud.commandframework.annotations.CommandMethod;
import tc.oc.pgm.lib.cloud.commandframework.annotations.CommandPermission;

public class WarnCommand extends CommunityCommand {

  private final ModerationFeature moderation;
  private final UsersFeature usernames;

  public WarnCommand() {
    this.moderation = Community.get().getFeatures().getModeration();
    this.usernames = Community.get().getFeatures().getUsers();
  }

  @CommandMethod("warn|w <target> [reason]")
  @CommandDescription("Warn a player for bad behavior")
  @CommandPermission(CommunityPermissions.WARN)
  public void warn(
      CommandAudience audience,
      @Argument("target") TargetPlayer target,
      @Argument("reason") String reason) {
    getTarget(target.getIdentifier(), usernames)
        .thenAccept(
            id -> {
              if (id.isPresent()) {
                moderation.punish(
                    PunishmentType.WARN,
                    id.get(),
                    audience,
                    reason,
                    null,
                    false,
                    isDisguised(audience));
              } else {
                audience.sendWarning(formatNotFoundComponent(target.getIdentifier()));
              }
            });
  }
}

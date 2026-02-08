package dev.pgm.community.moderation.commands;

import dev.pgm.community.Community;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.commands.player.TargetPlayer;
import dev.pgm.community.moderation.feature.ModerationFeature;
import dev.pgm.community.moderation.punishments.PunishmentType;
import dev.pgm.community.users.feature.UsersFeature;
import dev.pgm.community.utils.CommandAudience;
import tc.oc.pgm.lib.org.incendo.cloud.annotation.specifier.Greedy;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Argument;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Command;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.CommandDescription;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Permission;

public class NoteCommand extends CommunityCommand {

  private final ModerationFeature moderation;
  private final UsersFeature usernames;

  public NoteCommand() {
    this.moderation = Community.get().getFeatures().getModeration();
    this.usernames = Community.get().getFeatures().getUsers();
  }

  @Command("note|addnote <target> <reason>")
  @CommandDescription("Add a staff-only note to a player's record")
  @Permission(CommunityPermissions.NOTE)
  public void note(
      CommandAudience audience,
      @Argument("target") TargetPlayer target,
      @Argument("reason") @Greedy String reason) {
    getTarget(target.getIdentifier(), usernames).thenAccept(id -> {
      if (id.isPresent()) {
        moderation.punish(
            PunishmentType.NOTE, id.get(), audience, reason, null, false, isDisguised(audience));
      } else {
        audience.sendWarning(formatNotFoundComponent(target.getIdentifier()));
      }
    });
  }
}

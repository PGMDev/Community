package dev.pgm.community.moderation.commands;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Syntax;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.moderation.feature.ModerationFeature;
import dev.pgm.community.moderation.punishments.PunishmentType;
import dev.pgm.community.users.feature.UsersFeature;
import dev.pgm.community.utils.CommandAudience;

public class KickCommand extends CommunityCommand {

  @Dependency private ModerationFeature moderation;
  @Dependency private UsersFeature usernames;

  @CommandAlias("kick|k")
  @Description("Kick a player from the server")
  @Syntax("[player] [reason]")
  @CommandCompletion("@players")
  @CommandPermission(CommunityPermissions.KICK)
  public void kick(CommandAudience audience, String target, String reason) {
    getTarget(target, usernames)
        .thenAccept(
            id -> {
              if (id.isPresent()) {
                moderation.punish(
                    PunishmentType.KICK,
                    id.get(),
                    audience,
                    reason,
                    null,
                    false,
                    isVanished(audience));
              } else {
                audience.sendWarning(formatNotFoundComponent(target));
              }
            });
  }
}

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

public class WarnCommand extends CommunityCommand {

  @Dependency private ModerationFeature moderation;
  @Dependency private UsersFeature usernames;

  @CommandAlias("warn|w")
  @Description("Warn a player for bad behavior")
  @Syntax("[player] [reason]")
  @CommandCompletion("@players *")
  @CommandPermission(CommunityPermissions.WARN)
  public void warn(CommandAudience audience, String target, String reason) {
    getTarget(target, usernames)
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
                    isVanished(audience));
              } else {
                audience.sendWarning(formatNotFoundComponent(target));
              }
            });
  }
}

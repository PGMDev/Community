package dev.pgm.community.moderation.commands;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.moderation.feature.ModerationFeature;
import dev.pgm.community.moderation.punishments.PunishmentType;
import dev.pgm.community.users.feature.UsersFeature;
import dev.pgm.community.utils.CommandAudience;
import java.time.Duration;

@CommandAlias("ban|permban|pb")
@Description("Ban a player from the server")
@CommandPermission(CommunityPermissions.BAN)
public class BanCommand extends CommunityCommand {

  @Dependency private ModerationFeature moderation;
  @Dependency private UsersFeature usernames;

  @CommandAlias("tempban|tb")
  @Subcommand("temp|temporary|t")
  @Syntax("[player] [duration] [reason]")
  @CommandCompletion("@players 1d|3d|7d *")
  public void tempBan(CommandAudience audience, String target, Duration length, String reason) {
    getTarget(target, usernames)
        .thenAcceptAsync(
            id -> {
              if (id.isPresent()) {
                moderation.punish(
                    PunishmentType.TEMP_BAN,
                    id.get(),
                    audience,
                    reason,
                    length,
                    true,
                    isVanished(audience));
              } else {
                audience.sendWarning(formatNotFoundComponent(target));
              }
            });
  }

  @Default
  @Syntax("[player] [reason]")
  @CommandCompletion("@players")
  public void ban(CommandAudience audience, String target, String reason) {
    getTarget(target, usernames)
        .thenAcceptAsync(
            id -> {
              if (id.isPresent()) {
                moderation.punish(
                    PunishmentType.BAN,
                    id.get(),
                    audience,
                    reason,
                    null,
                    true,
                    isVanished(audience));
              } else {
                audience.sendWarning(formatNotFoundComponent(target));
              }
            });
  }
}

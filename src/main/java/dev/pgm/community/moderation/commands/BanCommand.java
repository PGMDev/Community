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
  @Syntax("[duration] [player] [reason]")
  @CommandCompletion("30m|6h|7d @players *")
  public void tempBan(CommandAudience audience, Duration length, String target, String reason) {
    moderation.punish(
        PunishmentType.TEMP_BAN,
        getTarget(target, usernames),
        audience,
        reason,
        length,
        true,
        isVanished(audience));
  }

  @CommandAlias("ipban")
  @Subcommand("ip|ipaddress")
  @Syntax("[player | IP address] [reason]")
  public void ipBan(
      CommandAudience audience,
      String target,
      String reason) {} // TODO: Maybe leave IP ban out of community and add to Bungee in a separate
  // plugin?

  @Default
  @Syntax("[player] [reason]")
  @CommandCompletion("@players")
  public void ban(CommandAudience audience, String target, String reason) {
    moderation.punish(
        PunishmentType.BAN,
        getTarget(target, usernames),
        audience,
        reason,
        null,
        true,
        isVanished(audience));
  }
}

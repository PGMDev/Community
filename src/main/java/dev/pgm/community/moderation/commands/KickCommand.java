package dev.pgm.community.moderation.commands;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Syntax;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.moderation.feature.ModerationFeature;
import dev.pgm.community.moderation.punishments.PunishmentType;
import dev.pgm.community.utils.CommandAudience;

public class KickCommand extends CommunityCommand {

  @Dependency private ModerationFeature moderation;

  @CommandAlias("kick|k")
  @Description("Kick a player from the server")
  @Syntax("[player] [reason]")
  @CommandCompletion("@players")
  @CommandPermission("TODO.KICK") // TODO: Permissions
  public void kick(CommandAudience audience, OnlinePlayer target, String reason) {
    moderation.punish(
        PunishmentType.KICK,
        target.getPlayer().getUniqueId(),
        audience,
        reason,
        null,
        false,
        isVanished(audience));
  }
}

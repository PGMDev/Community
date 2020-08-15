package dev.pgm.community.moderation.commands;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Syntax;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.moderation.feature.ModerationFeature;
import dev.pgm.community.moderation.punishments.PunishmentType;
import dev.pgm.community.utils.CommandAudience;

public class WarnCommand extends CommunityCommand {

  @Dependency private ModerationFeature moderation;

  @CommandAlias("warn|w")
  @Description("Warn a player for bad behavior")
  @Syntax("[player] [reason]")
  @CommandCompletion("@players")
  @CommandPermission(CommunityPermissions.WARN) // TODO: Permissions
  public void warn(CommandAudience sender, OnlinePlayer target, String reason) {
    moderation.punish(
        PunishmentType.WARN,
        target.getPlayer().getUniqueId(),
        sender,
        reason,
        null,
        false,
        isVanished(sender));
  }
}

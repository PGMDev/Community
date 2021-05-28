package dev.pgm.community.moderation.commands;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Flags;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.moderation.feature.PGMPunishmentIntegration;
import dev.pgm.community.moderation.tools.TeleportToolManager;
import org.bukkit.entity.Player;

public class ToolCommand extends CommunityCommand {

  private TeleportToolManager tools;

  public ToolCommand(PGMPunishmentIntegration integration) {
    this.tools = integration.getToolManager();
  }

  @CommandAlias("tptarget|tptg|tg")
  @Description("Target a player for the player hook tool")
  @CommandCompletion("@players")
  @CommandPermission(CommunityPermissions.STAFF)
  public void targetCommand(Player sender, @Flags("other") Player target) {
    tools.targetPlayer(sender, target);
  }
}

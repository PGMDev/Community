package dev.pgm.community.moderation.commands;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Flags;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.moderation.feature.PGMPunishmentIntegration;
import dev.pgm.community.moderation.tools.ModerationTools;
import dev.pgm.community.utils.PGMUtils;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.match.Match;

public class ToolCommand extends CommunityCommand {

  private final ModerationTools tools;

  public ToolCommand(PGMPunishmentIntegration integration) {
    this.tools = integration.getTools();
  }

  @CommandAlias("tptarget|tptg|tg")
  @Description("Target a player for the player hook tool")
  @CommandCompletion("@players")
  @CommandPermission(CommunityPermissions.STAFF)
  public void targetCommand(Player sender, @Flags("other") Player target) {
    tools.getTeleportHook().targetPlayer(sender, target);
  }

  @CommandAlias("modtools|mtools")
  @Description("Give moderator tools to observer")
  @CommandPermission(CommunityPermissions.STAFF)
  public void modTools(Player sender) {
    Match match = PGMUtils.getMatch();
    if (match != null && match.getPlayer(sender) != null && match.getPlayer(sender).isObserving()) {
      tools.giveTools(sender);
    }
  }
}

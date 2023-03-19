package dev.pgm.community.moderation.commands;

import static net.kyori.adventure.text.Component.text;

import dev.pgm.community.Community;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.moderation.tools.ModerationTools;
import dev.pgm.community.utils.CommandAudience;
import dev.pgm.community.utils.PGMUtils;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.lib.cloud.commandframework.annotations.Argument;
import tc.oc.pgm.lib.cloud.commandframework.annotations.CommandDescription;
import tc.oc.pgm.lib.cloud.commandframework.annotations.CommandMethod;
import tc.oc.pgm.lib.cloud.commandframework.annotations.CommandPermission;

public class ToolCommand extends CommunityCommand {

  private final ModerationTools tools;

  public ToolCommand() {
    this.tools = Community.get().getFeatures().getModeration().getTools();
  }

  @CommandMethod("tptarget|tptg|tg <target>")
  @CommandDescription("Target a player for the player hook tool")
  @CommandPermission(CommunityPermissions.STAFF)
  public void targetCommand(
      CommandAudience sender, Player player, @Argument("target") Player target) {
    if (isDisabled(sender)) return;
    tools.getTeleportHook().targetPlayer(player, target);
  }

  @CommandMethod("modtools|mtools")
  @CommandDescription("Give moderator tools to observer")
  @CommandPermission(CommunityPermissions.STAFF)
  public void modTools(CommandAudience sender, Player player) {
    if (isDisabled(sender)) return;

    Match match = PGMUtils.getMatch();
    if (match != null
        && match.getPlayer(player) != null
        && match.getPlayer(player).isObserving()
        && tools != null) {
      tools.giveTools(player);
    }
  }

  private boolean isDisabled(CommandAudience sender) {
    if (tools == null) {
      sender.sendWarning(text("Moderation Tools are not enabled!"));
      return true;
    }

    return false;
  }
}

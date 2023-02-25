package dev.pgm.community.teleports;

import static net.kyori.adventure.text.Component.text;

import dev.pgm.community.Community;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.utils.CommandAudience;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.lib.cloud.commandframework.annotations.Argument;
import tc.oc.pgm.lib.cloud.commandframework.annotations.CommandDescription;
import tc.oc.pgm.lib.cloud.commandframework.annotations.CommandMethod;
import tc.oc.pgm.lib.cloud.commandframework.annotations.CommandPermission;
import tc.oc.pgm.util.text.TextException;

public class TeleportCommand extends CommunityCommand {

  private final TeleportFeature teleport;

  public TeleportCommand() {
    this.teleport = Community.get().getFeatures().getTeleports();
  }

  @CommandMethod("tp|teleport <target>")
  @CommandDescription("Teleport to another player")
  @CommandPermission(CommunityPermissions.TELEPORT)
  public void teleportCommand(
      CommandAudience viewer, Player sender, @Argument("target") Player target) {
    teleport.teleport(viewer, sender, target);
  }

  @CommandMethod("tpall|tpa <target>")
  @CommandDescription("Teleport all players to the target player")
  @CommandPermission(CommunityPermissions.TELEPORT_ALL)
  public void teleportAll(CommandAudience viewer, @Argument("target") Player target) {
    PlayerSelection selection = getPlayers(viewer, "*");

    if (selection.getPlayers().isEmpty()) {
      selection.sendNoPlayerComponent(viewer);
      return;
    }

    teleport.teleport(viewer, selection.getPlayers(), target, selection.getText());
  }

  @CommandMethod("tpteam <team> <target>")
  @CommandDescription("Teleport all players on the given team to the target player")
  @CommandPermission(CommunityPermissions.TELEPORT_OTHERS)
  public void teleportTeam(
      CommandAudience viewer, @Argument("team") Party team, @Argument("target") Player target) {
    Set<Player> players =
        team.getPlayers().stream().map(MatchPlayer::getBukkit).collect(Collectors.toSet());

    if (players.isEmpty()) {
      viewer.sendWarning(
          text().append(team.getName()).append(text(" has no players to teleport")).build());
      return;
    }

    teleport.teleport(viewer, players, target, team.getName());
  }

  @CommandMethod("tphere|bring|tph <target>")
  @CommandDescription("Teleport players to you")
  @CommandPermission(CommunityPermissions.TELEPORT_OTHERS)
  public void teleportHereCommand(
      CommandAudience viewer, Player sender, @Argument("target") Player target) {
    teleportCommand(viewer, target, sender);
  }

  @CommandMethod("tplocation|tpl|tploc <coords> [target]")
  @CommandDescription("Teleport to specific coordinates")
  @CommandPermission(CommunityPermissions.TELEPORT_LOCATION)
  public void teleportLocation(
      CommandAudience viewer,
      @Argument("coords") Location location,
      @Argument("target") Player target) {
    if (target != null) {
      teleport.teleport(viewer, target, location);
      return;
    }

    if (!viewer.isPlayer()) TextException.playerOnly();

    teleport.teleport(viewer, viewer.getPlayer(), location);
  }
}

package dev.pgm.community.teleports;

import static net.kyori.adventure.text.Component.translatable;

import dev.pgm.community.Community;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.utils.CommandAudience;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import tc.oc.pgm.lib.cloud.commandframework.annotations.Argument;
import tc.oc.pgm.lib.cloud.commandframework.annotations.CommandDescription;
import tc.oc.pgm.lib.cloud.commandframework.annotations.CommandMethod;
import tc.oc.pgm.lib.cloud.commandframework.annotations.CommandPermission;

public class TeleportCommand extends CommunityCommand {

  private final TeleportFeature teleport;

  public TeleportCommand() {
    this.teleport = Community.get().getFeatures().getTeleports();
  }

  // TARGET SELECTORS

  // /tp * <single>
  // /tp ? <single>
  // /tp team=[name] <single>
  @CommandMethod("tp|teleport <target> [others]")
  @CommandDescription("Teleport to another player")
  @CommandPermission(CommunityPermissions.TELEPORT)
  public void teleportCommand(
      CommandAudience viewer,
      @Argument("target") String target1,
      @Argument("others") String target2) {

    if (viewer.isPlayer()) {
      Player sender = viewer.getPlayer();
      if (target2 == null) {
        Player player = getSinglePlayer(viewer, target1, true);
        if (player != null) {
          teleport.teleport(viewer, sender, player);
        }
        return;
      }
    }

    if (!viewer.getSender().hasPermission(CommunityPermissions.TELEPORT_OTHERS)) {
      viewer.sendWarning(translatable("misc.noPermission"));
      return;
    }

    if (target2 != null) {
      PlayerSelection targets = getPlayers(viewer, target1);

      Player player2 = getSinglePlayer(viewer, target2, true);
      if (!targets.getPlayers().isEmpty() && player2 != null) {
        teleport.teleport(viewer, targets.getPlayers(), player2, targets.getText());
      } else {
        targets.sendNoPlayerComponent(viewer);
      }
    }
  }

  @CommandMethod("tphere|bring|tph <target>")
  @CommandDescription("Teleport players to you")
  @CommandPermission(CommunityPermissions.TELEPORT_OTHERS)
  public void teleportHereCommand(
      CommandAudience viewer, Player sender, @Argument("target") String target) {
    teleportCommand(viewer, target, sender.getName());
  }

  @CommandMethod("tplocation|tpl|tploc <coords> [target]")
  @CommandDescription("Teleport to specific coordinates")
  @CommandPermission(CommunityPermissions.TELEPORT_LOCATION)
  public void teleportLocation(
      CommandAudience viewer,
      @Argument("coords") Location location,
      @Argument("target") String target) {
    if (target != null) {
      PlayerSelection targets = this.getPlayers(viewer, target);
      teleport.teleport(viewer, targets.getPlayers(), location, targets.getText());
    } else if (viewer.isPlayer()) {
      teleport.teleport(viewer, viewer.getPlayer(), location);
    }
  }
}

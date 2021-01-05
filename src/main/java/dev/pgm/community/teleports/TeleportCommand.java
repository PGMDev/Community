package dev.pgm.community.teleports;

import static net.kyori.adventure.text.Component.translatable;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Flags;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Syntax;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.utils.CommandAudience;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class TeleportCommand extends CommunityCommand {

  @Dependency private TeleportFeature teleport;

  // NEW TARGET SELECTORS

  // /tp * <single>
  // /tp ? <single>
  // /tp team=[name] <single>

  @CommandAlias("tp|teleport")
  @Description("Teleport to another player")
  @Syntax("<player> <other player> | <*, ?=1, team=Name, name1,name2...> <target>")
  @CommandCompletion("@visible @visible")
  @CommandPermission(CommunityPermissions.TELEPORT)
  public void teleportCommand(CommandAudience viewer, String target1, @Optional String target2) {

    if (viewer.isPlayer()) {
      Player sender = (Player) viewer.getSender();
      if (target2 == null) {
        Player player = getSinglePlayer(viewer, target1);
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

      Player player2 = getSinglePlayer(viewer, target2);
      if (!targets.getPlayers().isEmpty() && player2 != null) {
        teleport.teleport(viewer, targets.getPlayers(), player2, targets.getText());
      } else {
        targets.sendNoPlayerComponent(viewer);
      }
    }
  }

  @CommandAlias("tphere|bring|tph")
  @Description("Teleport players to you")
  @Syntax("[player] - Player to teleport")
  @CommandCompletion("@players")
  @CommandPermission(CommunityPermissions.TELEPORT_OTHERS)
  public void teleportHereCommand(CommandAudience viewer, String target) {
    if (viewer.isPlayer()) {
      teleportCommand(viewer, target, viewer.getPlayer().getName());
    }
  }

  @CommandAlias("tplocation|tpl|tploc")
  @Description("Teleport to specific coordinates")
  @Syntax("[x,y,z] [target]")
  @CommandCompletion("@players")
  @CommandPermission(CommunityPermissions.TELEPORT_LOCATION)
  public void teleportLocation(
      CommandAudience viewer,
      Location location,
      @Optional @Flags("other,defaultself") Player target) {
    if (target != null) {
      teleport.teleport(viewer, target, location);
    }
  }
}

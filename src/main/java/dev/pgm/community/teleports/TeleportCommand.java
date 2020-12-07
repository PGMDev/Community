package dev.pgm.community.teleports;

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
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.types.PlayerComponent;

public class TeleportCommand extends CommunityCommand {

  @Dependency private TeleportFeature teleport;

  @CommandAlias("tp|teleport")
  @Description("Teleport to another player")
  @Syntax("<player> <other player>")
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
      viewer.sendWarning(TranslatableComponent.of("misc.noPermission"));
      return;
    }

    if (target2 != null) {
      Player player1 = getSinglePlayer(viewer, target1);
      Player player2 = getSinglePlayer(viewer, target2);
      if (player1 != null && player2 != null) {
        teleport.teleport(viewer, player1, player2);
      }
    }
  }

  @CommandAlias("tphere|bring|tph")
  @Description("Teleport a player to you")
  @Syntax("[player] - Player to teleport")
  @CommandCompletion("@players")
  @CommandPermission(CommunityPermissions.TELEPORT_OTHERS)
  public void teleportHereCommand(CommandAudience viewer, String target) {
    if (viewer.isPlayer()) {
      Player sender = viewer.getPlayer();
      Player player = getSinglePlayer(viewer, target);

      if (player != null) {
        teleport.teleport(
            viewer,
            player,
            sender,
            null,
            TextComponent.builder()
                .append("Teleported ")
                .append(PlayerComponent.of(player, NameStyle.FANCY))
                .append(" to your location")
                .color(TextColor.GRAY)
                .build(),
            true);
      }
    }
  }

  @CommandAlias("tpall|tpa|bringall")
  @Description("Teleport everyone to you or another player")
  @Syntax("[target] - Player to teleport everyone to")
  @CommandCompletion("@players")
  @CommandPermission(CommunityPermissions.TELEPORT_ALL)
  public void teleportAllCommand(CommandAudience viewer, @Optional String target) {
    if (viewer.isPlayer()) {
      if (target != null && getSinglePlayer(viewer, target) == null)
        return; // Return if target is not found
      Player targetPlayer = target != null ? getSinglePlayer(viewer, target) : viewer.getPlayer();
      Bukkit.getOnlinePlayers()
          .forEach(p -> teleport.teleport(viewer, p, targetPlayer, null, null, false));
      int size = Bukkit.getOnlinePlayers().size();
      Component locName =
          targetPlayer.equals(viewer.getPlayer())
              ? TextComponent.of("your location")
              : PlayerComponent.of(targetPlayer, NameStyle.FANCY);
      viewer.sendMessage(
          TextComponent.builder()
              .append("Teleported ")
              .append(TextComponent.of(size, TextColor.YELLOW, TextDecoration.BOLD))
              .append(" player" + (size != 1 ? "s " : " "))
              .append("to ")
              .append(locName)
              .color(TextColor.GRAY)
              .build());
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

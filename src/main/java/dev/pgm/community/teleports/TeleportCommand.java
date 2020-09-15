package dev.pgm.community.teleports;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Flags;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Syntax;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.utils.CommandAudience;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
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
  public void teleportCommand(
      CommandAudience viewer, @Flags("other") OnlinePlayer player, @Optional OnlinePlayer target) {

    if (viewer.isPlayer()) {
      Player sender = (Player) viewer.getSender();
      if (target == null) {
        teleport.teleport(viewer, sender, player.getPlayer());
        return;
      }
    }

    if (target != null) {
      teleport.teleport(viewer, player.getPlayer(), target.getPlayer());
    }
  }

  @CommandAlias("tphere|bring|tph")
  @Description("Teleport a player to you")
  @Syntax("[player] - Player to teleport")
  @CommandCompletion("@players")
  @CommandPermission(CommunityPermissions.TELEPORT_OTHERS)
  public void teleportHereCommand(CommandAudience viewer, @Flags("other") Player target) {
    if (viewer.isPlayer()) {
      Player sender = viewer.getPlayer();

      teleport.teleport(
          viewer,
          target,
          sender,
          null,
          TextComponent.builder()
              .append("Teleported ")
              .append(PlayerComponent.of(target, NameStyle.FANCY))
              .append(" to your location")
              .color(TextColor.GRAY)
              .build());
    }
  }

  @CommandAlias("tplocation|tpl|tploc")
  @Description("Teleport to specific coordinates")
  @Syntax("[x,y,z] [target]")
  @CommandCompletion("@players")
  public void teleportLocation(
      CommandAudience viewer,
      Location location,
      @Optional @Flags("other,defaultself") Player target) {
    if (target != null) {
      teleport.teleport(viewer, target, location);
    }
  }
}

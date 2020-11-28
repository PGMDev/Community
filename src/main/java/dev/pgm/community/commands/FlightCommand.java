package dev.pgm.community.commands;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.utils.CommandAudience;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.entity.Player;

public class FlightCommand extends CommunityCommand {

  @CommandAlias("fly|flight")
  @Description("Toggle your flight mode")
  @CommandPermission(CommunityPermissions.FLIGHT)
  public void fly(CommandAudience audience, Player player) {
    boolean fly = player.getAllowFlight();
    player.setAllowFlight(!fly);
    player.setFlying(!fly);
    audience.sendMessage(
        TextComponent.builder()
            .append("Toggled flying mode ", TextColor.GRAY)
            .append(
                player.isFlying() ? "On" : "Off",
                player.isFlying() ? TextColor.GREEN : TextColor.RED)
            .build());
  }

  @CommandAlias("flyspeed")
  @Description("Adjust your flight speed")
  @CommandPermission(CommunityPermissions.FLIGHT_SPEED)
  public void flySpeed(CommandAudience audience, Player player, @Optional Float speed) {
    if (speed == null) {
      audience.sendMessage(
          TextComponent.builder()
              .append("Your flight speed is ", TextColor.GRAY)
              .append(TextComponent.of(player.getFlySpeed() * 100, TextColor.GREEN))
              .build());
      return;
    }
    player.setFlySpeed(Math.abs(Math.min(speed, 10) / 10));
    audience.sendMessage(
        TextComponent.builder()
            .append("Flight speed set to ", TextColor.GRAY)
            .append(TextComponent.of(player.getFlySpeed() * 100, TextColor.GREEN))
            .build());
  }
}

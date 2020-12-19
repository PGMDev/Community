package dev.pgm.community.commands;

import static net.kyori.adventure.text.Component.text;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.utils.CommandAudience;
import net.kyori.adventure.text.format.NamedTextColor;
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
        text("Toggled flying mode ", NamedTextColor.GRAY)
            .append(
                text(
                    player.isFlying() ? "On" : "Off",
                    player.isFlying() ? NamedTextColor.GREEN : NamedTextColor.RED)));
  }

  @CommandAlias("flyspeed")
  @Description("Adjust your flight speed")
  @CommandPermission(CommunityPermissions.FLIGHT_SPEED)
  public void flySpeed(CommandAudience audience, Player player, @Optional Float speed) {
    if (speed == null) {
      audience.sendMessage(
          text("Your flight speed is ", NamedTextColor.GRAY)
              .append(text(player.getFlySpeed() * 100, NamedTextColor.GREEN)));
      return;
    }
    player.setFlySpeed(Math.abs(Math.min(speed, 10) / 10));
    audience.sendMessage(
        text("Flight speed set to ", NamedTextColor.GRAY)
            .append(text(player.getFlySpeed() * 100, NamedTextColor.GREEN)));
  }
}

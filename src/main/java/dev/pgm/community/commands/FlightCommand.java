package dev.pgm.community.commands;

import static net.kyori.adventure.text.Component.text;

import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.utils.CommandAudience;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import tc.oc.pgm.lib.cloud.commandframework.annotations.Argument;
import tc.oc.pgm.lib.cloud.commandframework.annotations.CommandDescription;
import tc.oc.pgm.lib.cloud.commandframework.annotations.CommandMethod;
import tc.oc.pgm.lib.cloud.commandframework.annotations.CommandPermission;
import tc.oc.pgm.util.Audience;

public class FlightCommand extends CommunityCommand {

  @CommandMethod("fly|flight [target]")
  @CommandDescription("Toggle flight mode for yourself or other players")
  @CommandPermission(CommunityPermissions.FLIGHT)
  public void fly(CommandAudience sender, @Argument("target") String targets) {
    if (sender.isPlayer() && targets == null) {
      toggleFlight(sender.getPlayer());
    } else {
      PlayerSelection selection = getPlayers(sender, targets);
      if (!selection.getPlayers().isEmpty()) {
        selection.getPlayers().forEach(this::toggleFlight);
        sender.sendMessage(
            text()
                .append(text("Flight has been toggled for "))
                .append(selection.getText())
                .color(NamedTextColor.GRAY)
                .build());
      } else {
        selection.sendNoPlayerComponent(sender);
      }
    }
  }

  @CommandMethod("flyspeed [speed]")
  @CommandDescription("Adjust your flight speed")
  @CommandPermission(CommunityPermissions.FLIGHT_SPEED)
  public void flySpeed(CommandAudience audience, Player player, @Argument("speed") Float speed) {
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

  private void toggleFlight(Player player) {
    boolean fly = player.getAllowFlight();
    player.setAllowFlight(!fly);
    player.setFlying(!fly);
    Audience.get(player)
        .sendMessage(
            text("Toggled flying mode ", NamedTextColor.GRAY)
                .append(
                    text(
                        player.isFlying() ? "On" : "Off",
                        player.isFlying() ? NamedTextColor.GREEN : NamedTextColor.RED)));
  }
}

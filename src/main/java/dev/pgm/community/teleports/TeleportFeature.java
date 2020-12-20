package dev.pgm.community.teleports;

import static net.kyori.adventure.text.Component.text;

import dev.pgm.community.feature.Feature;
import dev.pgm.community.utils.CommandAudience;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.PlayerComponent;

/** TeleportFeature - Teleport players to other locations * */
public interface TeleportFeature extends Feature {

  default void teleport(CommandAudience sender, Player teleporter, Player target) {
    teleport(sender, teleporter, target, formatTeleportPlayerMessage(target), null, true);
  }

  default void teleport(CommandAudience sender, Player teleporter, Location target) {
    teleport(
        sender,
        teleporter,
        target,
        text("Teleported to ").append(formatLocation(target)).color(NamedTextColor.GRAY));
  }

  void teleport(
      CommandAudience audience, Player teleporter, Location target, @Nullable Component message);

  void teleport(
      CommandAudience audience,
      Player teleporter,
      Player target,
      @Nullable Component teleporterMsg,
      @Nullable Component targetMsg,
      boolean senderFeedback);

  default Component formatTeleportPlayerMessage(Player target) {
    return text("Teleported to ")
        .append(PlayerComponent.player(target, NameStyle.FANCY))
        .color(NamedTextColor.GRAY);
  }

  default Component formatLocation(Location location) {
    return text(("("))
        .append(text(location.getBlockX(), NamedTextColor.AQUA))
        .append(text(", "))
        .append(text(location.getBlockY(), NamedTextColor.AQUA))
        .append(text(", "))
        .append(text(location.getBlockZ(), NamedTextColor.AQUA))
        .append(text(")"))
        .color(NamedTextColor.GRAY);
  }
}

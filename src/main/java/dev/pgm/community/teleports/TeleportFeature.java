package dev.pgm.community.teleports;

import dev.pgm.community.feature.Feature;
import dev.pgm.community.utils.CommandAudience;
import javax.annotation.Nullable;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.types.PlayerComponent;

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
        TextComponent.builder()
            .append("Teleported to ")
            .append(formatLocation(target))
            .color(TextColor.GRAY)
            .build());
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
    return TextComponent.builder()
        .append("Teleported to ")
        .append(PlayerComponent.of(target, NameStyle.FANCY))
        .color(TextColor.GRAY)
        .build();
  }

  default Component formatLocation(Location location) {
    return TextComponent.builder()
        .append("(")
        .append(TextComponent.of(location.getBlockX(), TextColor.AQUA))
        .append(", ")
        .append(TextComponent.of(location.getBlockY(), TextColor.AQUA))
        .append(", ")
        .append(TextComponent.of(location.getBlockZ(), TextColor.AQUA))
        .append(")")
        .color(TextColor.GRAY)
        .build();
  }
}

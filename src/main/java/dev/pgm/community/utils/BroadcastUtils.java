package dev.pgm.community.utils;

import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;

import dev.pgm.community.CommunityPermissions;
import javax.annotation.Nullable;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import tc.oc.pgm.util.Audience;

public class BroadcastUtils {

  public static final Component RIGHT_DIV = text("\u00BB");
  public static final Component LEFT_DIV = text("\u00AB");

  public static final Component BROADCAST_DIV =
      text().append(space()).append(RIGHT_DIV).append(space()).build();

  public static final Component ADMIN_CHAT_PREFIX =
      text()
          .append(text("[", NamedTextColor.WHITE))
          .append(text("A", NamedTextColor.GOLD))
          .append(text("]", NamedTextColor.WHITE))
          .append(space())
          .build();

  public static void sendAdminChatMessage(Component message) {
    sendAdminChatMessage(message, null);
  }

  public static void sendAdminChatMessage(Component message, @Nullable Sound sound) {
    Component formatted = text().append(ADMIN_CHAT_PREFIX).append(message).build();
    Bukkit.getOnlinePlayers().stream()
        .filter(player -> player.hasPermission(CommunityPermissions.STAFF))
        .map(Audience::get)
        .forEach(
            viewer -> {
              viewer.sendMessage(formatted);
              if (sound != null) {
                // TODO: Look into settings for Sounds?
                viewer.playSound(sound);
              }
            });
    Audience.get(Bukkit.getConsoleSender()).sendMessage(formatted);
  }

  public static void sendGlobalMessage(Component message) {
    Bukkit.getOnlinePlayers().stream().map(Audience::get).forEach(p -> p.sendMessage(message));
    Audience.get(Bukkit.getConsoleSender()).sendMessage(message);
  }

  public static void sendGlobalWarning(Component message) {
    Bukkit.getOnlinePlayers().stream().map(Audience::get).forEach(p -> p.sendWarning(message));
    Audience.get(Bukkit.getConsoleSender()).sendMessage(message);
  }
}

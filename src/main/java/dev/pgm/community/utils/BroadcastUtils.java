package dev.pgm.community.utils;

import dev.pgm.community.CommunityPermissions;
import javax.annotation.Nullable;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.Bukkit;
import tc.oc.pgm.util.chat.Audience;
import tc.oc.pgm.util.chat.Sound;

public class BroadcastUtils {

  public static final Component RIGHT_DIV = TextComponent.of("\u00BB");
  public static final Component LEFT_DIV = TextComponent.of("\u00AB");

  public static final Component BROADCAST_DIV =
      TextComponent.builder()
          .append(" ")
          .append(RIGHT_DIV)
          .append(" ")
          .color(TextColor.GRAY)
          .build();

  public static final TextComponent ADMIN_CHAT_PREFIX =
      TextComponent.builder()
          .append("[", TextColor.WHITE)
          .append("A", TextColor.GOLD)
          .append("] ", TextColor.WHITE)
          .build();

  public static void sendAdminChatMessage(Component message) {
    sendAdminChatMessage(message, null);
  }

  public static void sendAdminChatMessage(Component message, @Nullable Sound sound) {
    Component formatted = TextComponent.builder().append(ADMIN_CHAT_PREFIX).append(message).build();
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

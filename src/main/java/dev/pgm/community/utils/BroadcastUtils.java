package dev.pgm.community.utils;

import javax.annotation.Nullable;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.Bukkit;
import tc.oc.pgm.util.chat.Audience;
import tc.oc.pgm.util.chat.Sound;

public class BroadcastUtils {

  public static final Component BROADCAST_DIV = TextComponent.of(" \u00BB ", TextColor.GRAY);

  public static final TextComponent ADMIN_CHAT_PREFIX =
      TextComponent.builder()
          .append("[", TextColor.WHITE)
          .append("A", TextColor.GOLD)
          .append("] ", TextColor.WHITE)
          .build();

  public static void sendAdminChat(Component message, @Nullable Sound sound) {
    Component formatted = TextComponent.builder().append(ADMIN_CHAT_PREFIX).append(message).build();
    Bukkit.getOnlinePlayers().stream()
        .filter(player -> player.hasPermission("community.staff"))
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

  public static void sendGlobalChat(Component formatted) {
    Bukkit.getOnlinePlayers().stream().map(Audience::get).forEach(p -> p.sendMessage(formatted));
    Audience.get(Bukkit.getConsoleSender()).sendMessage(formatted);
  }
}

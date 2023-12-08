package dev.pgm.community.utils;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.title.Title.title;

import dev.pgm.community.Community;
import dev.pgm.community.CommunityPermissions;
import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title.Times;
import net.kyori.adventure.util.Ticks;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.bukkit.ViaUtils;

public class BroadcastUtils {

  public static final Component RIGHT_DIV = text("\u00BB");
  public static final Component LEFT_DIV = text("\u00AB");

  public static final Component BROADCAST_DIV =
      text().append(space()).append(RIGHT_DIV).append(space()).build();

  private static final Component ADMIN_CHAT_PREFIX =
      text()
          .append(text("[", NamedTextColor.WHITE))
          .append(text("A", NamedTextColor.GOLD))
          .append(text("]", NamedTextColor.WHITE))
          .build();

  private static boolean isLocal(String server) {
    return Community.get().getServerId().equalsIgnoreCase(server);
  }

  private static Component formatPrefix(String server, Component message) {
    TextComponent.Builder builder = text();

    if (server == null || isLocal(server)) {
      builder.append(ADMIN_CHAT_PREFIX);
    } else {
      builder
          .append(text("[", NamedTextColor.WHITE))
          .append(text("A ", NamedTextColor.GOLD))
          .append(text(StringUtils.capitalize(server.toLowerCase()), NamedTextColor.GREEN))
          .append(text("]", NamedTextColor.WHITE));
    }

    return builder.append(space()).append(message).build();
  }

  public static void sendAdminChatMessage(Component message, String permission) {
    sendAdminChatMessage(message, null, null, permission);
  }

  public static void sendAdminChatMessage(
      Component message, @Nullable Sound sound, String permission) {
    sendAdminChatMessage(message, null, sound, permission);
  }

  public static void sendAdminChatMessage(
      Component message,
      @Nullable String server,
      @Nullable Sound sound,
      @Nullable String permission) {
    Component formatted = formatPrefix(server, message);
    Bukkit.getOnlinePlayers().stream()
        .filter(
            player -> {
              boolean isStaff = player.hasPermission(CommunityPermissions.STAFF);
              if (permission != null) {
                return isStaff || player.hasPermission(permission);
              }

              return isStaff;
            })
        .map(Audience::get)
        .forEach(
            viewer -> {
              viewer.sendMessage(formatted);
              if (sound != null) {
                viewer.playSound(sound);
              }
            });
    Audience.console().sendMessage(formatted);
  }

  public static void sendMultiLineGlobal(List<Component> lines, Sound sound) {
    sendMultiLineGlobal(lines);
    playGlobalSound(sound);
  }

  public static void sendMultiLineGlobal(List<Component> lines) {
    lines.forEach(BroadcastUtils::sendGlobalMessage);
  }

  public static void sendGlobalMessage(Component message) {
    sendGlobalMessage(message, null);
  }

  public static void sendGlobalMessage(Component message, @Nullable Sound sound) {
    Bukkit.getOnlinePlayers().stream()
        .map(Audience::get)
        .forEach(
            p -> {
              p.sendMessage(message);
              if (sound != null) {
                p.playSound(sound);
              }
            });
    Audience.get(Bukkit.getConsoleSender()).sendMessage(message);
  }

  public static void sendGlobalWarning(Component message) {
    Bukkit.getOnlinePlayers().stream().map(Audience::get).forEach(p -> p.sendWarning(message));
    Audience.get(Bukkit.getConsoleSender()).sendMessage(message);
  }

  public static void sendGlobalTitle(
      @Nullable Component title, @Nullable Component subTitle, int stay) {
    sendGlobalTitle(title, subTitle, stay, null);
  }

  public static void sendGlobalTitle(
      @Nullable Component title, @Nullable Component subTitle, int stay, @Nullable Sound sound) {
    Bukkit.getOnlinePlayers().stream()
        .forEach(
            p -> {
              Audience viewer = Audience.get(p);
              // Support legacy players
              if (ViaUtils.getProtocolVersion(p) <= ViaUtils.VERSION_1_7) {
                viewer.sendMessage(text().append(title).append(subTitle));
              }
              viewer.showTitle(
                  title(
                      title == null ? empty() : title,
                      subTitle == null ? empty() : subTitle,
                      Times.of(Ticks.duration(5), Ticks.duration(20 * stay), Ticks.duration(15))));

              if (sound != null) {
                viewer.playSound(sound);
              }
            });
  }

  public static void playSelectSound(Sound sound, Predicate<Player> filter) {
    Bukkit.getOnlinePlayers().stream().filter(filter).forEach(player -> playSound(player, sound));
  }

  public static void playGlobalSound(Sound sound) {
    Bukkit.getOnlinePlayers().forEach(player -> playSound(player, sound));
  }

  public static void playSound(Player player, Sound sound) {
    Audience.get(player).playSound(sound);
  }
}

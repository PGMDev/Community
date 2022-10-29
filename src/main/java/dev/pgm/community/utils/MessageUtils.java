package dev.pgm.community.utils;

import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static tc.oc.pgm.util.text.TemporalComponent.duration;

import com.google.common.collect.Lists;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import tc.oc.pgm.util.LegacyFormatUtils;
import tc.oc.pgm.util.bukkit.BukkitUtils;
import tc.oc.pgm.util.text.TextTranslations;

public class MessageUtils {

  public static final String TOKEN_SYMBOL = "✪";
  public static final Component DENY = text("\u2715", NamedTextColor.DARK_RED);
  public static final Component ACCEPT = text("\u2714", NamedTextColor.GREEN);
  public static final Component WARNING = text("\u26a0", NamedTextColor.YELLOW);
  public static final Component TOKEN = text(TOKEN_SYMBOL, NamedTextColor.GOLD);

  public static String formatKickScreenMessage(String headerTitle, List<Component> lines) {
    List<Component> message = Lists.newArrayList();

    Component header =
        text(LegacyFormatUtils.horizontalLineHeading(headerTitle, ChatColor.DARK_GRAY));

    Component footer =
        text(
            LegacyFormatUtils.horizontalLine(
                ChatColor.DARK_GRAY, LegacyFormatUtils.MAX_CHAT_WIDTH));

    message.add(header); // Header Line - FIRST
    lines.forEach(message::add); // Add messages
    message.add(footer); // Footer Line - LAST

    return TextTranslations.translateLegacy(
        Component.join(text("\n" + ChatColor.RESET), message), null);
  }

  public static Component formatUnseen(String target) {
    return text()
        .append(text(target, NamedTextColor.DARK_AQUA))
        .append(text(" has never joined the server", NamedTextColor.RED))
        .build();
    // TODO: translate
  }

  public static Component formatNotFriend(String target) {
    return text()
        .append(text("You are not friends with ", NamedTextColor.RED))
        .append(text(target, NamedTextColor.DARK_AQUA))
        .build();
  }

  public static Component formatTokenTransaction(int amount, Component message) {
    return formatTokenTransaction(amount, message, null);
  }

  public static Component formatTokenTransaction(int amount, Component message, Component hover) {
    boolean add = amount > 0;
    TextComponent.Builder builder =
        text()
            .append(
                text(
                    add ? "+" : "-",
                    add ? NamedTextColor.GREEN : NamedTextColor.RED,
                    TextDecoration.BOLD))
            .append(text(Math.abs(amount) + " ", NamedTextColor.YELLOW, TextDecoration.BOLD))
            .append(TOKEN)
            .append(space())
            .append(message);

    if (hover != null) {
      builder.hoverEvent(HoverEvent.showText(hover));
    }

    return builder.build();
  }

  public static Component formatTimeLeft(
      Duration totalTime, Instant lastTime, NamedTextColor color) {
    Duration timeLeft = totalTime.minus(Duration.between(lastTime, Instant.now()));
    return text().append(duration(timeLeft, color)).build();
  }

  public static Component color(String message, Object... args) {
    return LegacyComponentSerializer.legacyAmpersand().deserialize(String.format(message, args));
  }

  public static List<String> colorizeList(List<String> list) {
    return list.stream().map(BukkitUtils::colorize).collect(Collectors.toList());
  }

  public static String format(String format, Object... args) {
    return String.format(
        ChatColor.translateAlternateColorCodes('&', format != null ? format : ""), args);
  }
}

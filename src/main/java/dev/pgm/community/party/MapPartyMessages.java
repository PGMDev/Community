package dev.pgm.community.party;

import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static tc.oc.pgm.util.bukkit.BukkitUtils.colorize;
import static tc.oc.pgm.util.text.PlayerComponent.player;
import static tc.oc.pgm.util.text.TemporalComponent.duration;

import com.google.common.collect.Lists;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.utils.BroadcastUtils;
import dev.pgm.community.utils.MessageUtils;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent.Builder;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.TextFormatter;
import tc.oc.pgm.util.text.TextTranslations;

public class MapPartyMessages {

  public static final Component CREATION_ERROR =
      text("Unable to create new map party! Stop the existing map party first", NamedTextColor.RED);

  public static final Component MISSING_ERROR =
      text("No active map party found!", NamedTextColor.RED);

  public static final Component STARTED_ERROR =
      text("The map party has already started!", NamedTextColor.RED);

  public static final Component RESTART_ERROR =
      text("Unable to restart map party.", NamedTextColor.RED);

  public static final Component USE_SETPOOL_ERROR =
      text("Unable to set map pool while map party is active!", NamedTextColor.RED);

  public static final Component REGULAR_PARTY_ONLY_ERROR =
      text("This feature can only be used during a regular map party!", NamedTextColor.RED);

  public static final Component CUSTOM_PARTY_ONLY_ERROR =
      text("This feature can only be used during a custom map party!", NamedTextColor.RED);

  public static final Component REQUIRE_ONE_MAP_ERROR =
      text("At least one map is required for the map party to function", NamedTextColor.RED);

  public static final Component SET_DESCRIPTION_REMINDER =
      text()
          .append(text("Don't forget to set a map party description. Click "))
          .append(text("["))
          .append(text("here", NamedTextColor.AQUA))
          .append(text("]"))
          .color(NamedTextColor.GRAY)
          .clickEvent(ClickEvent.suggestCommand("/event setdesc"))
          .hoverEvent(
              HoverEvent.showText(text("Click to set event description", NamedTextColor.GRAY)))
          .build();

  public static final Component CREATE_PARTY_BROADCAST =
      text()
          .append(text("created a new map party"))
          .hoverEvent(
              HoverEvent.showText(text("Click to view map party info", NamedTextColor.GRAY)))
          .clickEvent(ClickEvent.runCommand("/event"))
          .color(NamedTextColor.GRAY)
          .build();

  public static Component getAddHostError(Player player) {
    return text()
        .append(player(player, NameStyle.FANCY))
        .append(text(" is unable to become a map party host!", NamedTextColor.RED))
        .build();
  }

  public static Component getExistingHostError(Player player) {
    return text()
        .append(player(player, NameStyle.FANCY))
        .append(text(" is already a party host!", NamedTextColor.RED))
        .build();
  }

  public static Component getUnknownPreset(String presetName) {
    return text()
        .append(text("There are no presets named "))
        .append(text(presetName, NamedTextColor.AQUA))
        .color(NamedTextColor.RED)
        .build();
  }

  public static Component getEventStatusAlert(MapParty party, MapPartyStatusType type) {
    return text()
        .append(type.getNameComponent())
        .append(space())
        .append(party.getStyledName())
        .color(NamedTextColor.GRAY)
        .build();
  }

  public static String formatTime(MapParty party) {
    if (party.getLength() == null) return ChatColor.YELLOW + "No timelimit";
    String duration = TextTranslations.translateLegacy(duration(party.getLength()).build(), null);

    if (party.getStartTime() == null) return ChatColor.GOLD + duration;
    Duration timeElapsed = Duration.between(party.getStartTime(), Instant.now());
    Duration timeRemaining = party.getLength().minus(timeElapsed);
    return ChatColor.GREEN
        + TextTranslations.translateLegacy(duration(timeRemaining).build(), null);
  }

  public static void broadcastHostAction(Component sender, Component action) {
    broadcastHostAction(sender, action, null);
  }

  public static void broadcastHostAction(
      Component sender, Component action, @Nullable Component value) {
    Builder broadcast = text().append(sender).append(space()).append(action);

    if (value != null) {
      broadcast.append(space()).append(value);
    }
    broadcast.color(NamedTextColor.GRAY);

    BroadcastUtils.sendExclusiveChatMessage(broadcast.build(), CommunityPermissions.PARTY);
  }

  public static List<Component> getWelcome(MapParty party, MapPartyConfig config) {
    return getMultiLinePartyMessage(
        party,
        NamedTextColor.GREEN,
        text(colorize(party.getDescription()), NamedTextColor.BLUE),
        text()
            .append(text("The event will last for another "))
            .append(text(formatTime(party)))
            .append(text("!"))
            .color(NamedTextColor.GRAY)
            .build(),
        getWelcomeComponent(
            config.getWelcomeLine(), config.getWelcomeHover(), config.getWelcomeCommand()));
  }

  private static Component getWelcomeComponent(String message, String hover, String command) {
    return text()
        .append(MessageUtils.color(message))
        .hoverEvent(HoverEvent.showText(MessageUtils.color(hover)))
        .clickEvent(ClickEvent.runCommand(command))
        .build();
  }

  public static List<Component> getGoodbye(MapParty party, MapPartyConfig mapPartyConfig) {
    return getMultiLinePartyMessage(
        party, NamedTextColor.GOLD, text(colorize(mapPartyConfig.getGoodbyeMessage())));
  }

  private static List<Component> getMultiLinePartyMessage(
      MapParty party, NamedTextColor color, Component... messages) {
    List<Component> lines = Lists.newArrayList();
    lines.add(TextFormatter.horizontalLineHeading(null, party.getStyledName(), color));
    for (int i = 0; i < messages.length; i++) {
      lines.add(messages[i]);
      if (i + 1 < messages.length) {
        lines.add(text(" "));
      }
    }
    lines.add(TextFormatter.horizontalLine(color, TextFormatter.MAX_CHAT_WIDTH));
    return lines;
  }
}

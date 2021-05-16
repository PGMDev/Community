package dev.pgm.community.moderation.punishments;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

import dev.pgm.community.users.feature.UsersFeature;
import dev.pgm.community.utils.Sounds;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.text.PlayerComponent;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.TextFormatter;
import tc.oc.pgm.util.text.TextTranslations;

public class PunishmentFormats {

  // Broadcast
  public static CompletableFuture<Component> formatBroadcast(
      Punishment punishment, String server, UsersFeature users) {
    CompletableFuture<Component> broadcast = new CompletableFuture<>();
    CompletableFuture<Component> issuer =
        users.renderUsername(punishment.getIssuerId(), NameStyle.FANCY, null);
    CompletableFuture<Component> target =
        users.renderUsername(Optional.of(punishment.getTargetId()), NameStyle.FANCY, null);
    CompletableFuture.allOf(issuer, target)
        .thenAcceptAsync(
            x -> {
              Component msg = punishment.formatBroadcast(issuer.join(), target.join());
              broadcast.complete(msg);
            });
    return broadcast;
  }

  public static Component formatBanEvasion(Player player, UUID bannedId, Component banned) {
    return text()
        .append(
            translatable(
                "moderation.similarIP.loginEvent",
                NamedTextColor.GRAY,
                PlayerComponent.player(player, NameStyle.FANCY),
                banned))
        .hoverEvent(
            HoverEvent.showText(text("Click to issue ban evasion punishment", NamedTextColor.RED)))
        .clickEvent(
            ClickEvent.runCommand(
                "/ban "
                    + player.getName()
                    + " Ban Evasion - ("
                    + ChatColor.stripColor(TextTranslations.translateLegacy(banned, null) + ")")))
        .build();
  }

  public static void sendNoPlayBanWelcome(Player player, Punishment punishment, String appeal) {
    Audience viewer = Audience.get(player);
    viewer.sendMessage(
        TextFormatter.horizontalLineHeading(
            player,
            translatable("misc.warning", NamedTextColor.DARK_RED, TextDecoration.BOLD),
            NamedTextColor.YELLOW));
    viewer.sendMessage(
        text()
            .append(
                text(
                    "You have been"
                        + (punishment.getDuration() != null ? " temporarily" : "")
                        + " banned for ",
                    NamedTextColor.GRAY))
            .append(text(punishment.getReason(), NamedTextColor.RED)));
    viewer.sendMessage(text().append(empty()));
    if (punishment.getDuration() != null) {
      viewer.sendMessage(punishment.getExpireDateMessage());
      viewer.sendMessage(empty());
    }

    Component restricted =
        text()
            .append(text("You are no longer allowed to "))
            .append(text("play", NamedTextColor.YELLOW))
            .append(text(", "))
            .append(text("chat", NamedTextColor.YELLOW))
            .append(text(", or use "))
            .append(text("commands", NamedTextColor.YELLOW))
            .color(NamedTextColor.GRAY)
            .build();

    viewer.sendMessage(restricted);
    viewer.sendMessage(empty());
    viewer.sendMessage(text(appeal));
    viewer.sendMessage(text().append(text()));
    viewer.sendMessage(
        TextFormatter.horizontalLineHeading(
            player,
            translatable("misc.warning", NamedTextColor.DARK_RED, TextDecoration.BOLD),
            NamedTextColor.YELLOW));
    viewer.playSound(Sounds.WARN_SOUND);
  }

  public static Component formatBanDenyError(Punishment punishment, String action) {
    return text()
        .append(text("You are unable to "))
        .append(text(action))
        .append(text(" while banned for: "))
        .append(text(punishment.getReason(), NamedTextColor.RED))
        .color(NamedTextColor.GRAY)
        .build();
  }
}

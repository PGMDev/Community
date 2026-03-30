package dev.pgm.community.requests.sponsor;

import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.event.HoverEvent.showText;
import static tc.oc.pgm.util.player.PlayerComponent.player;
import static tc.oc.pgm.util.text.TemporalComponent.duration;

import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.requests.feature.RequestFeature;
import dev.pgm.community.utils.BroadcastUtils;
import dev.pgm.community.utils.PGMUtils;
import dev.pgm.community.utils.PGMUtils.MapSizeBounds;
import java.time.Duration;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.util.named.MapNameStyle;
import tc.oc.pgm.util.named.NameStyle;

public class SponsorComponents {

  public static ComponentLike getMapSizeBoundsComponent(MapSizeBounds bounds) {
    return text("(", NamedTextColor.YELLOW)
        .append(text(bounds.lowerBound(), NamedTextColor.GOLD))
        .append(text("-"))
        .append(text(bounds.upperBound(), NamedTextColor.GOLD))
        .append(text(")"));
  }

  public static Component getWrongSizeMapError(Component mapName, MapSizeBounds mapBounds) {

    Component remove = text()
        .append(text("[", NamedTextColor.GRAY))
        .append(text("Remove", NamedTextColor.YELLOW))
        .append(text("]", NamedTextColor.GRAY))
        .hoverEvent(HoverEvent.showText(
            text("Click to remove map from sponsor queue", NamedTextColor.GRAY)))
        .clickEvent(ClickEvent.runCommand("/sponsor cancel"))
        .build();

    return text()
        .append(mapName)
        .append(text(" no longer fits the online player count "))
        .append(getMapSizeBoundsComponent(mapBounds))
        .append(text(". We'll try again after the next match, or you can "))
        .append(remove)
        .append(text(" and select a new map to sponsor."))
        .build();
  }

  public static Component getMapSelectionSizeWarning(MapInfo map, MapSizeBounds mapBounds) {
    return text()
        .append(map.getStyledName(MapNameStyle.COLOR))
        .append(text(" (Max of ", NamedTextColor.YELLOW)
            .append(text(PGMUtils.getMapMaxSize(map), NamedTextColor.RED))
            .append(text(")")))
        .append(text(" does not fit the online player count "))
        .append(getMapSizeBoundsComponent(mapBounds))
        .append(newline())
        .append(text("Please request a different map!"))
        .build();
  }

  public static Component getTokenRefreshMessage(int amount, int total, boolean daily) {
    return text()
        .append(RequestFeature.TOKEN)
        .append(text(" Recieved "))
        .append(text("+" + amount, NamedTextColor.GREEN, TextDecoration.BOLD))
        .append(text(" sponsor token" + (amount != 1 ? "s" : "")))
        .append(text(" ("))
        .append(text("Total: ", NamedTextColor.GRAY))
        .append(text(total, NamedTextColor.YELLOW, TextDecoration.BOLD))
        .append(text(")"))
        .color(NamedTextColor.GOLD)
        .hoverEvent(HoverEvent.showText(text("Next token refresh will be in ", NamedTextColor.GRAY)
            .append(duration(Duration.ofDays(daily ? 1 : 7), NamedTextColor.YELLOW))))
        .build();
  }

  public static Component getSponsoredJoinMessage(UUID playerId) {
    return text()
        .append(text(" Sponsored by "))
        .append(player(playerId, NameStyle.FANCY))
        .color(NamedTextColor.GRAY)
        .build();
  }

  public static Component getQueuePositionMessage(int index) {
    return text()
        .append(text("Queue position "))
        .append(text("#" + index, NamedTextColor.YELLOW))
        .append(text(" Use "))
        .append(text("/sponsor queue", NamedTextColor.AQUA))
        .append(text(" to track status"))
        .color(NamedTextColor.GRAY)
        .clickEvent(ClickEvent.runCommand("/sponsor queue"))
        .hoverEvent(showText(text("Click to view queue status", NamedTextColor.GRAY)))
        .build();
  }

  public static Component getSuccessfulSponsorMessage(MapInfo map) {
    return text()
        .append(RequestFeature.SPONSOR)
        .append(text(" You've sponsored ", NamedTextColor.YELLOW))
        .append(map.getStyledName(MapNameStyle.COLOR))
        .build();
  }

  private void alertStaff(Player player, MapInfo map, boolean sponsor) {
    Component alert = text()
        .append(player(player, NameStyle.FANCY))
        .append(text(" has "))
        .append(text(sponsor ? "sponsored " : "requested "))
        .append(map.getStyledName(MapNameStyle.COLOR))
        .color(NamedTextColor.YELLOW)
        .build();

    BroadcastUtils.sendAdminChatMessage(alert, CommunityPermissions.REQUEST_STAFF);
  }

  public static Component getSponsorAdminChatAlert(Player player, MapInfo map) {
    return text()
        .append(player(player, NameStyle.FANCY))
        .append(text(" has "))
        .append(text("sponsored "))
        .append(map.getStyledName(MapNameStyle.COLOR))
        .color(NamedTextColor.YELLOW)
        .build();
  }
}

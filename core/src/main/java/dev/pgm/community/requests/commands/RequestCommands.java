package dev.pgm.community.requests.commands;

import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.util.text.TemporalComponent.duration;

import dev.pgm.community.Community;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.requests.feature.RequestFeature;
import dev.pgm.community.utils.BroadcastUtils;
import dev.pgm.community.utils.CommandAudience;
import dev.pgm.community.utils.PaginatedComponentResults;
import java.time.Duration;
import java.util.Comparator;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.lib.org.incendo.cloud.annotation.specifier.Greedy;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Argument;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Command;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.CommandDescription;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Default;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Permission;
import tc.oc.pgm.util.StreamUtils;
import tc.oc.pgm.util.named.MapNameStyle;
import tc.oc.pgm.util.text.TextFormatter;

public class RequestCommands extends CommunityCommand {

  private final RequestFeature requests;

  public RequestCommands() {
    this.requests = Community.get().getFeatures().getRequests();
  }

  @Command("request|req <map>")
  @CommandDescription("Request a map")
  @Permission(CommunityPermissions.REQUEST)
  public void request(
      CommandAudience audience, Player sender, @Argument("map") @Greedy MapInfo map) {
    requests.request(sender, map);
  }

  @Command("mapcooldowns|mapcd|mapcooldown [page]")
  @CommandDescription("View a list of current map cooldowns")
  @Permission(CommunityPermissions.VIEW_MAP_COOLDOWNS)
  public void viewCooldowns(
      CommandAudience audience, Player sender, @Argument("page") @Default("1") int page) {
    var library = PGM.get().getMapLibrary();
    record MapWithCooldown(MapInfo map, Duration cooldown) {}

    var maps = StreamUtils.of(library.getMaps())
        .map(map -> new MapWithCooldown(map, requests.getApproximateCooldown(map)))
        .filter(mcd -> mcd.cooldown.toSeconds() > 0)
        .sorted(Comparator.comparing(MapWithCooldown::cooldown))
        .toList();

    int resultsPerPage = 10;
    int pages = (maps.size() + resultsPerPage - 1) / resultsPerPage;

    Component paginated = TextFormatter.paginate(
        text("Active Cooldowns"), page, pages, NamedTextColor.DARK_AQUA, NamedTextColor.AQUA, true);

    Component formattedTitle = TextFormatter.horizontalLineHeading(
        audience.getSender(), paginated, NamedTextColor.DARK_PURPLE, 250);

    new PaginatedComponentResults<MapWithCooldown>(formattedTitle, resultsPerPage) {
      @Override
      public Component format(MapWithCooldown mcd, int index) {
        var map = mcd.map;

        return map.getStyledName(MapNameStyle.COLOR_WITH_AUTHORS)
            .clickEvent(ClickEvent.runCommand("/map " + map.getName()))
            .hoverEvent(HoverEvent.showText(translatable(
                "command.maps.hover", NamedTextColor.GRAY, map.getStyledName(MapNameStyle.COLOR))))
            .append(BroadcastUtils.BROADCAST_DIV.color(NamedTextColor.GOLD))
            .append((mcd.cooldown().toSeconds() > 0
                    ? duration(mcd.cooldown(), NamedTextColor.YELLOW).append(text(" remaining"))
                    : text("available soon"))
                .color(NamedTextColor.GRAY));
      }

      @Override
      public Component formatEmpty() {
        return text("There are no maps with active cooldowns!", NamedTextColor.RED);
      }
    }.display(audience.getAudience(), maps, page);
  }

  @Command("requests|reqs [page]")
  @CommandDescription("View and manage map requests")
  @Permission(CommunityPermissions.REQUEST_STAFF)
  public void listRequests(CommandAudience audience, @Argument("page") @Default("1") int page) {
    Map<MapInfo, Integer> requestCounts = requests.getRequests();

    int resultsPerPage = 8;
    int pages = (requestCounts.size() + resultsPerPage - 1) / resultsPerPage;

    Component paginated = TextFormatter.paginate(
        text("Map Requests"), page, pages, NamedTextColor.DARK_AQUA, NamedTextColor.AQUA, true);

    Component formattedTitle = TextFormatter.horizontalLineHeading(
        audience.getSender(), paginated, NamedTextColor.DARK_PURPLE, 250);

    new PaginatedComponentResults<MapInfo>(formattedTitle, resultsPerPage) {
      @Override
      public Component format(MapInfo map, int index) {
        Component mapName = map.getStyledName(MapNameStyle.COLOR)
            .clickEvent(ClickEvent.runCommand("/map " + map.getName()))
            .hoverEvent(HoverEvent.showText(translatable(
                "command.maps.hover", NamedTextColor.GRAY, map.getStyledName(MapNameStyle.COLOR))));

        int requestCount = requestCounts.get(map);
        Component count = text(requestCount, NamedTextColor.DARK_AQUA, TextDecoration.BOLD);

        Component setButton = getRequestsButton(
            "Set", "Click to setnext", NamedTextColor.GREEN, "/sn " + map.getName());
        Component voteButton = getRequestsButton(
            "Vote",
            "Click to add to the next vote",
            NamedTextColor.LIGHT_PURPLE,
            "/vote add " + map.getName());
        Component removeButton = getRequestsButton(
            "\u2715",
            "Click to remove all requests",
            NamedTextColor.RED,
            "/reqs clear " + map.getName());

        return text()
            .append(text((index + 1) + ". "))
            .append(mapName)
            .append(BroadcastUtils.BROADCAST_DIV)
            .append(count)
            .append(text(" request" + (requestCount != 1 ? "s " : " ")))
            .append(setButton)
            .append(space())
            .append(voteButton)
            .append(space())
            .append(removeButton)
            .color(NamedTextColor.GRAY)
            .build();
      }

      @Override
      public Component formatEmpty() {
        return text("No map requests have been made!", NamedTextColor.RED);
      }
    }.display(audience.getAudience(), requestCounts.keySet(), page);
  }

  @Command("requests|reqs clear [map]")
  @CommandDescription("Clear map requests")
  @Permission(CommunityPermissions.REQUEST_STAFF)
  public void clearRequests(CommandAudience audience, @Argument("map") @Greedy MapInfo map) {
    if (map != null) {
      int removalCount = requests.clearRequests(map);
      Component removed = text()
          .append(text(" ("))
          .append(text(removalCount, NamedTextColor.RED))
          .append(text(")"))
          .color(NamedTextColor.GRAY)
          .build();
      BroadcastUtils.sendAdminChatMessage(
          text()
              .append(audience.getStyledName())
              .append(text(" has cleared all requests for "))
              .append(map.getStyledName(MapNameStyle.COLOR))
              .append(removed)
              .color(NamedTextColor.GRAY)
              .build(),
          CommunityPermissions.REQUEST_STAFF);
    } else {
      requests.clearAllRequests();
      BroadcastUtils.sendAdminChatMessage(
          text()
              .append(audience.getStyledName())
              .append(text(" has cleared all map requests", NamedTextColor.GRAY))
              .build(),
          CommunityPermissions.REQUEST_STAFF);
    }
  }

  @Command("requests|reqs toggle")
  @CommandDescription("Toggle map requests")
  @Permission(CommunityPermissions.REQUEST_STAFF)
  public void toggleRequests(CommandAudience audience) {
    requests.toggleAccepting();
    BroadcastUtils.sendAdminChatMessage(
        text()
            .append(audience.getStyledName())
            .append(text(" has ", NamedTextColor.GRAY))
            .append(text(
                requests.isAccepting() ? "enabled" : "disabled",
                requests.isAccepting() ? NamedTextColor.GREEN : NamedTextColor.RED,
                TextDecoration.BOLD))
            .append(text(" map requests", NamedTextColor.GRAY))
            .build(),
        CommunityPermissions.REQUEST_STAFF);
  }

  private Component getRequestsButton(
      String text, String hover, NamedTextColor color, String command) {
    return text()
        .append(text("["))
        .append(text(text, color))
        .append(text("]"))
        .hoverEvent(HoverEvent.showText(text(hover, color)))
        .clickEvent(ClickEvent.runCommand(command))
        .build();
  }
}

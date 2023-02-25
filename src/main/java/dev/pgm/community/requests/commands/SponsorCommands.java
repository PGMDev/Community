package dev.pgm.community.requests.commands;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.util.player.PlayerComponent.player;
import static tc.oc.pgm.util.text.TemporalComponent.duration;

import com.google.common.collect.Sets;
import dev.pgm.community.Community;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.requests.RequestConfig;
import dev.pgm.community.requests.RequestProfile;
import dev.pgm.community.requests.SponsorRequest;
import dev.pgm.community.requests.commands.TokenCommands.TokenRefreshAmount;
import dev.pgm.community.requests.feature.RequestFeature;
import dev.pgm.community.utils.BroadcastUtils;
import dev.pgm.community.utils.CommandAudience;
import dev.pgm.community.utils.MessageUtils;
import dev.pgm.community.utils.PGMUtils;
import dev.pgm.community.utils.PaginatedComponentResults;
import dev.pgm.community.utils.VisibilityUtils;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.Phase;
import tc.oc.pgm.lib.cloud.commandframework.annotations.Argument;
import tc.oc.pgm.lib.cloud.commandframework.annotations.CommandDescription;
import tc.oc.pgm.lib.cloud.commandframework.annotations.CommandMethod;
import tc.oc.pgm.util.named.MapNameStyle;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.TextFormatter;

@CommandMethod("sponsor")
public class SponsorCommands extends CommunityCommand {

  private final RequestFeature requests;

  public SponsorCommands() {
    this.requests = Community.get().getFeatures().getRequests();
  }

  @CommandMethod("")
  public void info(CommandAudience audience, Player player) {
    Component header =
        TextFormatter.horizontalLineHeading(
            audience.getSender(),
            text("Sponsor", NamedTextColor.YELLOW, TextDecoration.BOLD),
            NamedTextColor.GOLD,
            TextFormatter.MAX_CHAT_WIDTH);

    Component footer =
        TextFormatter.horizontalLine(NamedTextColor.GOLD, TextFormatter.MAX_CHAT_WIDTH);

    RequestConfig config = ((RequestConfig) requests.getConfig());

    requests
        .getRequestProfile(player.getUniqueId())
        .thenAcceptAsync(
            profile -> {
              Component tokenBalance =
                  text()
                      .append(RequestFeature.TOKEN)
                      .append(text(" Token balance: "))
                      .append(text(profile.getSponsorTokens(), NamedTextColor.YELLOW))
                      .append(text(" / "))
                      .append(text(config.getMaxTokens(), NamedTextColor.GOLD))
                      .append(renderExtraInfo(player, profile))
                      .color(NamedTextColor.GRAY)
                      .clickEvent(ClickEvent.runCommand("/tokens"))
                      .hoverEvent(
                          HoverEvent.showText(text("Click to view tokens", NamedTextColor.GRAY)))
                      .build();

              Component buttons =
                  text()
                      .append(text("     "))
                      .append(
                          button(
                              "Map List",
                              NamedTextColor.DARK_AQUA,
                              "/sponsor maps 1",
                              "Click to view available maps as a chat list"))
                      .append(text("       "))
                      .append(
                          button(
                              "Map Menu",
                              NamedTextColor.GREEN,
                              "/sponsor menu",
                              "Click to view available maps as a GUI menu"))
                      .append(text("       "))
                      .append(
                          button(
                              "Queue",
                              NamedTextColor.DARK_GREEN,
                              "/sponsor queue",
                              "View a list of waiting sponsor requests ("
                                  + ChatColor.YELLOW
                                  + requests.getSponsorQueue().size()
                                  + ChatColor.GRAY
                                  + ")"))
                      .build();

              audience.sendMessage(header);

              // TOKEN BALANCE
              audience.sendMessage(tokenBalance);

              // Existing request status
              requests
                  .getPendingSponsor(player.getUniqueId())
                  .ifPresent(
                      sponsor -> {
                        int queueIndex = requests.queueIndex(sponsor);
                        boolean next = queueIndex == 0;

                        Component current =
                            text("Current Request: ", NamedTextColor.GRAY, TextDecoration.BOLD)
                                .append(
                                    button(
                                        "Cancel",
                                        NamedTextColor.RED,
                                        "/sponsor cancel",
                                        "Click to cancel this request"));
                        Component queue =
                            next
                                ? text(
                                    "(Will be added to the next vote)",
                                    NamedTextColor.GRAY,
                                    TextDecoration.ITALIC)
                                : text()
                                    .append(text("(Queue location: "))
                                    .append(text("#" + queueIndex, NamedTextColor.YELLOW))
                                    .append(text(")"))
                                    .color(NamedTextColor.GRAY)
                                    .hoverEvent(
                                        HoverEvent.showText(
                                            text(
                                                "Your request's location in the queue",
                                                NamedTextColor.GRAY)))
                                    .build();

                        audience.sendMessage(empty());
                        audience.sendMessage(current);
                        audience.sendMessage(
                            text()
                                .append(text(" - ", NamedTextColor.YELLOW))
                                .append(
                                    sponsor.getMap().getStyledName(MapNameStyle.COLOR_WITH_AUTHORS))
                                .build());
                        audience.sendMessage(queue);
                      });

              audience.sendMessage(empty());

              // Cooldown message
              if (!requests.canSponsor(player)) {
                audience.sendMessage(
                    text()
                        .append(text("Cooldown", NamedTextColor.GOLD, TextDecoration.BOLD))
                        .append(text(": ", NamedTextColor.GRAY))
                        .append(
                            MessageUtils.formatTimeLeft(
                                ((RequestConfig) requests.getConfig()).getSponsorCooldown(player),
                                profile.getLastSponsorTime(),
                                NamedTextColor.RED))
                        .color(NamedTextColor.GRAY)
                        .hoverEvent(
                            HoverEvent.showText(
                                text(
                                    "Time until you can sponsor another map", NamedTextColor.GRAY)))
                        .build());
                audience.sendMessage(empty());
              }

              // [Maps] [Queue]
              audience.sendMessage(buttons);

              audience.sendMessage(footer);
            });
  }

  @CommandMethod("request <map>")
  @CommandDescription("Sponsor a map request")
  public void sponsor(CommandAudience audience, Player sender, @Argument("map") MapInfo map) {
    requests.sponsor(sender, map);
  }

  @CommandMethod("cancel")
  public void cancel(CommandAudience audience, Player sender) {
    if (requests.cancelSponsorRequest(sender.getUniqueId())) {
      audience.sendMessage(text("Removed sponsor request!", NamedTextColor.GREEN));
    } else {
      audience.sendWarning(text("You don't have any pending sponsor requests to cancel"));
    }
  }

  @CommandMethod("menu")
  public void menu(CommandAudience audience, Player sender) {
    requests.openMenu(sender);
  }

  @CommandMethod("maps [page]")
  @CommandDescription("View a list of maps which can be sponsored")
  public void viewMapList(
      CommandAudience audience, @Argument(value = "page", defaultValue = "1") int page) {
    Set<MapInfo> maps =
        Sets.newHashSet(PGM.get().getMapLibrary().getMaps()).stream()
            .filter(PGMUtils::isMapSizeAllowed)
            .filter(m -> m.getPhase() != Phase.DEVELOPMENT)
            .filter(m -> !requests.hasMapCooldown(m))
            .collect(Collectors.toSet());

    int resultsPerPage = 8;
    int pages = (maps.size() + resultsPerPage - 1) / resultsPerPage;

    Component paginated =
        TextFormatter.paginate(
            text("Available Maps"),
            page,
            pages,
            NamedTextColor.DARK_AQUA,
            NamedTextColor.AQUA,
            true);

    Component formattedTitle =
        TextFormatter.horizontalLineHeading(
            audience.getSender(), paginated, NamedTextColor.DARK_PURPLE, 250);

    new PaginatedComponentResults<MapInfo>(formattedTitle, resultsPerPage) {
      @Override
      public Component format(MapInfo map, int index) {
        Component mapName =
            map.getStyledName(MapNameStyle.COLOR_WITH_AUTHORS)
                .clickEvent(ClickEvent.runCommand("/map " + map.getName()))
                .hoverEvent(
                    HoverEvent.showText(
                        translatable(
                            "command.maps.hover",
                            NamedTextColor.GRAY,
                            map.getStyledName(MapNameStyle.COLOR))));

        return text()
            .append(text((index + 1) + ". "))
            .append(mapName)
            .append(renderSponsorButton(audience.getSender(), map))
            .color(NamedTextColor.GRAY)
            .build();
      }

      @Override
      public Component formatEmpty() {
        return text("There are no available maps to sponsor!", NamedTextColor.RED);
      }
    }.display(audience.getAudience(), maps, page);

    // Add page button when more than 1 page
    if (pages > 1) {
      TextComponent.Builder buttons = text();

      if (page > 1) {
        buttons.append(
            text()
                .append(BroadcastUtils.LEFT_DIV.color(NamedTextColor.GOLD))
                .append(text(" Previous Page", NamedTextColor.BLUE))
                .hoverEvent(
                    HoverEvent.showText(text("Click to view previous page", NamedTextColor.GRAY)))
                .clickEvent(ClickEvent.runCommand("/sponsor maps " + (page - 1))));
      }

      if (page > 1 && page < pages) {
        buttons.append(text(" | ", NamedTextColor.DARK_GRAY));
      }

      if (page < pages) {
        buttons.append(
            text()
                .append(text("Next Page ", NamedTextColor.BLUE))
                .append(BroadcastUtils.RIGHT_DIV.color(NamedTextColor.GOLD))
                .hoverEvent(
                    HoverEvent.showText(text("Click to view next page", NamedTextColor.GRAY)))
                .clickEvent(ClickEvent.runCommand("/sponsor maps " + (page + 1))));
      }
      audience.sendMessage(
          TextFormatter.horizontalLineHeading(
              audience.getSender(), buttons.build(), NamedTextColor.DARK_PURPLE, 250));
    }
  }

  @CommandMethod("queue [page]")
  @CommandDescription("View the sponsored maps queue")
  public void viewQueue(
      CommandAudience audience, @Argument(value = "page", defaultValue = "1") int page) {
    Queue<SponsorRequest> queue = requests.getSponsorQueue();

    int resultsPerPage = ((RequestConfig) requests.getConfig()).getMaxQueue();
    int pages = (queue.size() + resultsPerPage - 1) / resultsPerPage;

    Component paginated =
        TextFormatter.paginate(
            text("Sponsor Queue"),
            page,
            pages,
            NamedTextColor.DARK_AQUA,
            NamedTextColor.AQUA,
            true);

    Component formattedTitle =
        TextFormatter.horizontalLineHeading(
            audience.getSender(), paginated, NamedTextColor.DARK_PURPLE, 250);

    new PaginatedComponentResults<SponsorRequest>(formattedTitle, resultsPerPage) {
      @Override
      public Component format(SponsorRequest sponsor, int index) {
        MapInfo map = sponsor.getMap();
        Component mapName =
            map.getStyledName(MapNameStyle.COLOR)
                .clickEvent(ClickEvent.runCommand("/map " + map.getName()))
                .hoverEvent(
                    HoverEvent.showText(
                        translatable(
                            "command.maps.hover",
                            NamedTextColor.GRAY,
                            map.getStyledName(MapNameStyle.COLOR))));

        Component playerName =
            VisibilityUtils.isDisguised(sponsor.getPlayerId())
                ? empty()
                : text()
                    .append(BroadcastUtils.BROADCAST_DIV)
                    .append(player(sponsor.getPlayerId(), NameStyle.FANCY))
                    .build();

        return text()
            .append(text((index + 1) + ". "))
            .append(mapName)
            .append(playerName)
            .color(NamedTextColor.GRAY)
            .build();
      }

      @Override
      public Component formatEmpty() {
        return text("There are no maps in the sponsor queue!", NamedTextColor.RED);
      }
    }.display(audience.getAudience(), queue, page);
  }

  private Component renderExtraInfo(Player player, RequestProfile profile) {
    RequestConfig config = (RequestConfig) requests.getConfig();
    TokenRefreshAmount info =
        TokenCommands.getTimeLeft(player, profile.getLastTokenRefreshTime(), requests);

    // If token refresh is disabled, display cooldown
    if (config.getDailyTokenAmount() == 0) {
      return text()
          .append(text(" | "))
          .append(text("Cooldown: "))
          .append(duration(config.getSponsorCooldown(player), NamedTextColor.GOLD))
          .hoverEvent(
              HoverEvent.showText(
                  text("This is your cooldown time between sponsor requests", NamedTextColor.GRAY)))
          .color(NamedTextColor.GRAY)
          .build();
    }

    if (info.getDuration() != null) {
      boolean canClaim = profile.getSponsorTokens() < config.getMaxTokens();

      if (canClaim) {
        return text()
            .append(text(" | "))
            .append(text("Next token ("))
            .append(text("+" + info.getAmount(), NamedTextColor.GREEN, TextDecoration.BOLD))
            .append(text("): "))
            .append(
                info.getDuration().isNegative()
                    ? text("Now! Please rejoin")
                    : duration(info.getDuration(), NamedTextColor.YELLOW))
            .build();
      } else {
        return text()
            .append(text(" | No tokens to claim"))
            .hoverEvent(
                HoverEvent.showText(
                    text(
                        "You have the max amount of sponsor tokens! To claim more you need to spend some first.",
                        NamedTextColor.RED)))
            .build();
      }
    }

    return empty();
  }

  private Component renderSponsorButton(CommandSender sender, MapInfo map) {
    if (sender instanceof Player) {
      Player player = (Player) sender;
      if (requests.getCached(player.getUniqueId()) != null) {
        RequestProfile profile = requests.getCached(player.getUniqueId());
        if (profile.getSponsorTokens() > 0) {
          return text()
              .append(BroadcastUtils.BROADCAST_DIV)
              .append(text("["))
              .append(RequestFeature.SPONSOR)
              .append(text("]"))
              .hoverEvent(
                  HoverEvent.showText(
                      text("Click to sponsor ", NamedTextColor.GRAY)
                          .append(map.getStyledName(MapNameStyle.COLOR))))
              .clickEvent(ClickEvent.runCommand("/sponsor add " + map.getName()))
              .color(NamedTextColor.GRAY)
              .build();
        }
      }
    }
    return empty();
  }
}

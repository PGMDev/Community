package dev.pgm.community.requests.commands;

import static dev.pgm.community.utils.PGMUtils.parseMapText;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.util.text.TemporalComponent.duration;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.requests.MapCooldown;
import dev.pgm.community.requests.RequestConfig;
import dev.pgm.community.requests.RequestProfile;
import dev.pgm.community.requests.feature.RequestFeature;
import dev.pgm.community.users.feature.UsersFeature;
import dev.pgm.community.utils.BroadcastUtils;
import dev.pgm.community.utils.CommandAudience;
import dev.pgm.community.utils.MessageUtils;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.named.MapNameStyle;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.TemporalComponent;
import tc.oc.pgm.util.text.TextFormatter;
import tc.oc.pgm.util.text.formatting.PaginatedComponentResults;

public class RequestCommands extends CommunityCommand {

  @Dependency private RequestFeature requests;
  @Dependency private UsersFeature users;

  @CommandAlias("request|req")
  @Description("Request a map")
  @Syntax("[map] - Name of map to request")
  @CommandCompletion("@maps")
  @CommandPermission(CommunityPermissions.REQUEST)
  public void request(CommandAudience audience, Player player, String mapName) {
    requests.request(player, parseMapText(mapName));
  }

  @CommandAlias("mapcooldowns|mapcd|mapcooldown")
  @Description("View a list of current map cooldowns")
  @CommandPermission(CommunityPermissions.VIEW_MAP_COOLDOWNS)
  public void viewCooldowns(CommandAudience audience, Player player, @Default("1") int page) {
    Map<MapInfo, MapCooldown> cooldowns = requests.getMapCooldowns();

    List<MapInfo> maps =
        cooldowns.entrySet().stream()
            .filter(e -> !e.getValue().hasExpired())
            .map(e -> e.getKey())
            .collect(Collectors.toList());

    Comparator<MapInfo> compare =
        (m1, m2) -> {
          MapCooldown m1C = cooldowns.get(m1);
          MapCooldown m2C = cooldowns.get(m2);
          Instant m1D = m1C.getEndTime();
          Instant m2D = m2C.getEndTime();

          return m2D.compareTo(m1D);
        };

    maps.sort(compare);

    int resultsPerPage = 10;
    int pages = (maps.size() + resultsPerPage - 1) / resultsPerPage;

    Component paginated =
        TextFormatter.paginate(
            text("Active Cooldowns"),
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
        MapCooldown cooldown = cooldowns.get(map);

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
            .append(mapName)
            .append(BroadcastUtils.BROADCAST_DIV.color(NamedTextColor.GOLD))
            .append(TemporalComponent.duration(cooldown.getTimeRemaining(), NamedTextColor.YELLOW))
            .append(text(" remaining"))
            .color(NamedTextColor.GRAY)
            .build();
      }

      @Override
      public Component formatEmpty() {
        return text("There are no maps with active cooldowns!", NamedTextColor.RED);
      }
    }.display(audience.getAudience(), maps, page);
  }

  @CommandAlias("requests|reqs")
  @Description("View and manage map requests")
  @CommandPermission(CommunityPermissions.STAFF)
  public class StaffRequestCommands extends CommunityCommand {

    @Default
    public void listRequests(CommandAudience audience, @Default("1") int page) {
      Map<MapInfo, Integer> requestCounts = requests.getRequests();

      int resultsPerPage = 8;
      int pages = (requestCounts.size() + resultsPerPage - 1) / resultsPerPage;

      Component paginated =
          TextFormatter.paginate(
              text("Map Requests"),
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
              map.getStyledName(MapNameStyle.COLOR)
                  .clickEvent(ClickEvent.runCommand("/map " + map.getName()))
                  .hoverEvent(
                      HoverEvent.showText(
                          translatable(
                              "command.maps.hover",
                              NamedTextColor.GRAY,
                              map.getStyledName(MapNameStyle.COLOR))));

          int requestCount = requestCounts.get(map);
          Component count = text(requestCount, NamedTextColor.DARK_AQUA, TextDecoration.BOLD);

          Component setButton =
              getRequestsButton(
                  "Set", "Click to setnext", NamedTextColor.GREEN, "/sn " + map.getName());
          Component voteButton =
              getRequestsButton(
                  "Vote",
                  "Click to add to the next vote",
                  NamedTextColor.LIGHT_PURPLE,
                  "/vote add " + map.getName());
          Component removeButton =
              getRequestsButton(
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

    @Subcommand("clear")
    @Syntax("[map name] - Empty to clear all")
    @Description("Clear map requests")
    public void clearRequests(CommandAudience audience, @Optional String mapName) {
      if (mapName != null) {
        MapInfo map = parseMapText(mapName);
        int removalCount = requests.clearRequests(map);
        Component removed =
            text()
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
                .build());
      } else {
        requests.clearAllRequests();
        BroadcastUtils.sendAdminChatMessage(
            text()
                .append(audience.getStyledName())
                .append(text(" has cleared all map requests", NamedTextColor.GRAY))
                .build());
      }
    }
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

  @CommandAlias("tokens|sponsortokens|token")
  @Description("View how many sponsor tokens you have")
  public class TokenCommands extends CommunityCommand {

    @Subcommand("give|award")
    @Syntax("[player] [token amount] - Amount of tokens to award")
    @Description("Give the targeted player sponsor tokens")
    @CommandCompletion("@players")
    @CommandPermission(CommunityPermissions.ADMIN)
    public void give(CommandAudience audience, String target, int amount) {
      getTarget(target, users)
          .thenAcceptAsync(
              targetId -> {
                if (targetId.isPresent()) {
                  RequestProfile profile = requests.getRequestProfile(targetId.get()).join();
                  if (profile != null) {
                    int total = profile.award(amount);
                    requests.update(profile);
                    audience.sendMessage(
                        text()
                            .append(MessageUtils.TOKEN)
                            .append(space())
                            .append(
                                users.renderUsername(profile.getPlayerId(), NameStyle.FANCY).join())
                            .append(text(" now has "))
                            .append(text(total, NamedTextColor.YELLOW, TextDecoration.BOLD))
                            .append(text(" sponsor token" + (total != 1 ? "s" : "")))
                            .color(NamedTextColor.GRAY)
                            .build());
                    return;
                  }
                }
                audience.sendWarning(formatNotFoundComponent(target));
              });
    }

    @Default
    @Subcommand("balance")
    @Description("Check your token balance")
    public void tokens(CommandAudience audience, @Optional String target) {
      if (target != null && audience.hasPermission(CommunityPermissions.TOKEN_BALANCE)) {
        getTarget(target, users)
            .thenAcceptAsync(
                uuid -> {
                  if (uuid.isPresent()) {
                    RequestProfile profile = requests.getRequestProfile(uuid.get()).join();
                    if (profile == null) {
                      audience.sendWarning(formatNotFoundComponent(target));
                      return;
                    }

                    Component name = users.renderUsername(uuid, NameStyle.FANCY).join();
                    sendTokenBalanceMessage(
                        audience.getAudience(), name, profile.getSponsorTokens());
                  } else {
                    audience.sendWarning(formatNotFoundComponent(target));
                  }
                });
      } else if (audience.isPlayer()) {
        Player player = audience.getPlayer();
        requests
            .getRequestProfile(player.getUniqueId())
            .thenAcceptAsync(
                profile -> {
                  int tokens = profile.getSponsorTokens();
                  sendTokenBalanceMessage(audience.getAudience(), null, tokens);
                  sendRefreshDuration(audience.getAudience(), player, profile);
                });
      } else {
        audience.sendWarning(text("Please provide a username to check the token balance of"));
      }
    }
  }

  private void sendTokenBalanceMessage(Audience viewer, Component name, int tokens) {
    viewer.sendMessage(
        text()
            .append(MessageUtils.TOKEN)
            .append(text(" "))
            .append(
                name == null ? text("You have ") : name.append(text(" has ", NamedTextColor.GRAY)))
            .append(text(tokens, NamedTextColor.YELLOW, TextDecoration.BOLD))
            .append(text(" sponsor token" + (tokens != 1 ? "s" : "") + "."))
            .color(NamedTextColor.GRAY)
            .build());
  }

  public static class TokenRefreshAmount {
    private Duration timeLeft;
    private int amount;

    public TokenRefreshAmount(Duration timeLeft, int amount) {
      this.timeLeft = timeLeft;
      this.amount = amount;
    }

    public Duration getDuration() {
      return timeLeft;
    }

    public int getAmount() {
      return amount;
    }
  }

  public static TokenRefreshAmount getTimeLeft(
      Player player, Instant lastRefresh, RequestFeature requests) {
    RequestConfig config = (RequestConfig) requests.getConfig();
    int amount = 0;
    Duration timeLeft = null;
    if (player.hasPermission(CommunityPermissions.TOKEN_DAILY)) {
      amount = config.getDailyTokenAmount();
      timeLeft = Duration.ofDays(1).minus(Duration.between(lastRefresh, Instant.now()));
    } else if (player.hasPermission(CommunityPermissions.TOKEN_WEEKLY)) {
      amount = config.getWeeklyTokenAmount();
      timeLeft = Duration.ofDays(7).minus(Duration.between(lastRefresh, Instant.now()));
    }
    return new TokenRefreshAmount(timeLeft, amount);
  }

  private void sendRefreshDuration(Audience viewer, Player player, RequestProfile profile) {
    if (!player.hasPermission(CommunityPermissions.REQUEST_SPONSOR)) return;

    if (profile.getLastTokenRefreshTime() == null) {
      viewer.sendWarning(
          text("Please rejoin the server to claim more tokens!", NamedTextColor.GOLD));
      return;
    }

    TokenRefreshAmount info = getTimeLeft(player, profile.getLastTokenRefreshTime(), requests);
    if (info.getDuration() != null) {
      if (profile.getSponsorTokens() >= ((RequestConfig) requests.getConfig()).getMaxTokens()) {
        viewer.sendMessage(text("Spend some tokens in order to claim more", NamedTextColor.GRAY));
        return;
      }

      if (info.getDuration().isNegative()) {
        viewer.sendMessage(
            text("Token refresh ready! Please rejoin the server to claim", NamedTextColor.GRAY));
        return;
      }

      viewer.sendMessage(
          text()
              .append(text("-  ", NamedTextColor.YELLOW))
              .append(text("Next token ("))
              .append(text("+", NamedTextColor.GREEN, TextDecoration.BOLD))
              .append(text(info.getAmount(), NamedTextColor.GREEN, TextDecoration.BOLD))
              .append(text(") in "))
              .append(duration(info.getDuration(), NamedTextColor.YELLOW))
              .color(NamedTextColor.GRAY)
              .build());
    }
  }
}

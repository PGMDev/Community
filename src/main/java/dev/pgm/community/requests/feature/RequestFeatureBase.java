package dev.pgm.community.requests.feature;

import static dev.pgm.community.utils.MessageUtils.formatTokenTransaction;
import static dev.pgm.community.utils.PGMUtils.compareMatchLength;
import static dev.pgm.community.utils.PGMUtils.getCurrentMap;
import static dev.pgm.community.utils.PGMUtils.isBlitz;
import static dev.pgm.community.utils.PGMUtils.isMapSizeAllowed;
import static dev.pgm.community.utils.PGMUtils.isMatchRunning;
import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.event.HoverEvent.showText;
import static tc.oc.pgm.util.text.PlayerComponent.player;
import static tc.oc.pgm.util.text.TemporalComponent.duration;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import dev.pgm.community.Community;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.feature.FeatureBase;
import dev.pgm.community.requests.MapCooldown;
import dev.pgm.community.requests.RequestConfig;
import dev.pgm.community.requests.RequestProfile;
import dev.pgm.community.requests.SponsorRequest;
import dev.pgm.community.requests.commands.RequestCommands;
import dev.pgm.community.users.feature.UsersFeature;
import dev.pgm.community.utils.BroadcastUtils;
import dev.pgm.community.utils.PGMUtils;
import dev.pgm.community.utils.Sounds;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapOrder;
import tc.oc.pgm.api.map.Phase;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.events.MapVoteWinnerEvent;
import tc.oc.pgm.rotation.MapPoolManager;
import tc.oc.pgm.rotation.VotingPool;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.named.MapNameStyle;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.TemporalComponent;

public abstract class RequestFeatureBase extends FeatureBase implements RequestFeature {

  private Cache<UUID, MapInfo> requests;

  private Cache<UUID, Instant> cooldown;

  private Map<MapInfo, MapCooldown> mapCooldown;

  private LinkedList<SponsorRequest> sponsors;

  private SponsorRequest currentSponsor;

  private PGMRequestIntegration integration;

  public RequestFeatureBase(
      RequestConfig config, Logger logger, String featureName, UsersFeature users) {
    super(config, logger, "Requests (" + featureName + ")");
    this.requests = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.HOURS).build();
    this.cooldown =
        CacheBuilder.newBuilder()
            .expireAfterWrite(config.getCooldown().getSeconds(), TimeUnit.SECONDS)
            .build();
    this.mapCooldown = Maps.newHashMap();
    this.sponsors = Lists.newLinkedList();
    this.currentSponsor = null;

    if (getConfig().isEnabled() && PGMUtils.isPGMEnabled()) {
      enable();
      this.integration = new PGMRequestIntegration(this, users);
    }
  }

  public RequestConfig getRequestConfig() {
    return (RequestConfig) getConfig();
  }

  @Override
  public Set<CommunityCommand> getCommands() {
    return getConfig().isEnabled() ? Sets.newHashSet(new RequestCommands()) : Sets.newHashSet();
  }

  @Override
  public Map<MapInfo, Integer> getRequests() {
    Map<MapInfo, Integer> requestCounts = Maps.newHashMap();
    requests
        .asMap()
        .values()
        .forEach(map -> requestCounts.put(map, (requestCounts.getOrDefault(map, 0) + 1)));
    return requestCounts;
  }

  @Override
  public SponsorRequest getCurrentSponsor() {
    return currentSponsor;
  }

  @Override
  public Queue<SponsorRequest> getSponsorQueue() {
    return sponsors;
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    onLogin(event)
        .thenAcceptAsync(
            profile -> {
              if (event.getPlayer().hasPermission(CommunityPermissions.REQUEST_SPONSOR)) {
                int refresh = 0;
                boolean daily = false;

                // Check permission to determine what amount to refresh
                // Always prefer the daily compared to the weekly (e.g sponsor inherits donor perms,
                // only give daily not weekly)
                if (event.getPlayer().hasPermission(CommunityPermissions.TOKEN_DAILY)) {
                  if (profile.hasDayElapsed()) {
                    refresh = getRequestConfig().getDailyTokenAmount();
                    daily = true;
                  }
                } else if (event.getPlayer().hasPermission(CommunityPermissions.TOKEN_WEEKLY)) {
                  if (profile.hasWeekElapsed()) {
                    refresh = getRequestConfig().getWeeklyTokenAmount();
                  }
                }

                // Refresh token amount as long as they have less than the max
                if (refresh > 0 && profile.getSponsorTokens() < getRequestConfig().getMaxTokens()) {
                  profile.refreshTokens(refresh); // Set new token amount
                  update(profile); // Save new token balance to database
                  sendDelayedTokenRefreshMessage(
                      event.getPlayer(), refresh, daily, profile.getSponsorTokens());
                }
              }
            });
  }

  @EventHandler
  public void onMatchEnd(MatchFinishEvent event) {
    if (currentSponsor != null) { // Reset current sponsor after match ends
      currentSponsor = null;
    }

    startNewMapCooldown(event.getMatch().getMap(), event.getMatch().getDuration());

    VotingPool pool = getVotingPool();

    if (pool != null) {
      if (!sponsors.isEmpty() && pool.getOptions().canAddVote()) {
        SponsorRequest nextRequest = sponsors.poll();
        if (nextRequest != null) {
          // Notify PGM of sponsored map
          getVotingPool()
              .getOptions()
              .addVote(nextRequest.getMap(), nextRequest.getPlayerId(), true);

          // Track the current sponsor
          this.currentSponsor = nextRequest;

          // Update profile
          getRequestProfile(nextRequest.getPlayerId())
              .thenAcceptAsync(
                  profile -> {
                    // Update RequestProfile with sponsor map info
                    profile.sponsor(nextRequest.getMap());
                    update(profile);
                  });

          // Alert online player if their sponsor request has been processed
          Player requester = Bukkit.getPlayer(nextRequest.getPlayerId());
          if (requester != null) {
            Audience player = Audience.get(requester);
            player.sendMessage(
                formatTokenTransaction(
                    -1,
                    text(
                        "Your sponsored map has been added to the vote!",
                        NamedTextColor.GREEN,
                        TextDecoration.BOLD),
                    canRefund(requester)
                        ? text(
                            "If your map wins the vote, you'll get your token back",
                            NamedTextColor.GRAY)
                        : null));
            player.playSound(Sounds.SPEND_TOKENS);
          }
        }
      }
    }
  }

  @EventHandler
  public void onVoteEnd(MapVoteWinnerEvent event) {
    if (currentSponsor != null) {
      Player player = Bukkit.getPlayer(currentSponsor.getPlayerId());

      // Same map = winner, refund the token even if offline
      if (currentSponsor.getMap().equals(event.getMap()) && currentSponsor.canRefund()) {
        getRequestProfile(currentSponsor.getPlayerId())
            .thenAcceptAsync(
                profile -> {
                  profile.award(1);
                  update(profile);

                  if (player != null) {
                    Audience viewer = Audience.get(player);
                    viewer.sendMessage(
                        formatTokenTransaction(
                            1,
                            text(
                                "Your sponsored map won the vote!",
                                NamedTextColor.GREEN,
                                TextDecoration.BOLD)));
                    viewer.playSound(Sounds.GET_TOKENS);
                  }
                });
      }
    }
  }

  @Override
  public void request(Player player, MapInfo map) {
    Audience viewer = Audience.get(player);

    // Cooldown
    if (hasCooldown(player, viewer)) return;

    if (requests.getIfPresent(player.getUniqueId()) != null) {
      MapInfo oldMap = requests.getIfPresent(player.getUniqueId());

      // Same map error
      if (map.equals(oldMap)) {
        viewer.sendWarning(
            text("You have already requested ").append(map.getStyledName(MapNameStyle.COLOR)));
        return;
      }

      // Replacement
      viewer.sendMessage(getReplaceMessage(oldMap, map));
    } else {
      viewer.sendMessage(getRequestMessage(player, map));
    }

    // Track request
    requests.put(player.getUniqueId(), map);

    // Start new cooldown
    cooldown.put(player.getUniqueId(), Instant.now());

    // Update profile
    getRequestProfile(player.getUniqueId())
        .thenAcceptAsync(
            profile -> {
              profile.request(map);
              update(profile);
            });

    // Alert the staff
    alertStaff(player, map, false);
  }

  @Override
  public void sponsor(Player player, MapInfo map) {
    Audience viewer = Audience.get(player);

    if (!isMatchRunning()) {
      viewer.sendWarning(text("Please wait until the next match has begun"));
      return;
    }

    if (getCurrentMap() != null && getCurrentMap().equals(map)) {
      viewer.sendWarning(text("Please select a different map"));
      return;
    }

    if (isBlitz()) {
      viewer.sendWarning(text("Sorry, sponsoring is disabled during blitz"));
      return;
    }

    // Ensure match is RUNNING and OVER one minute
    if (!compareMatchLength(Duration.ofMinutes(1))) {
      viewer.sendWarning(
          text()
              .append(text("The match just started!"))
              .append(newline())
              .append(text("Please try again after "))
              .append(text("1 minute", NamedTextColor.AQUA))
              .append(text(" of the match has passed"))
              .build());
      return;
    }

    // Don't allow developmental maps
    if (map.getPhase() == Phase.DEVELOPMENT) {
      viewer.sendWarning(
          text()
              .append(map.getStyledName(MapNameStyle.COLOR))
              .append(text(" may not be selected"))
              .build());
      return;
    }

    // Check if map has a cooldown
    if (hasMapCooldown(map)) {
      MapCooldown cooldown = mapCooldown.get(map);
      viewer.sendWarning(
          text()
              .append(text("This map can be sponsored in ", NamedTextColor.RED))
              .append(
                  TemporalComponent.duration(cooldown.getTimeRemaining(), NamedTextColor.YELLOW))
              .build());
      return;
    }

    // Check map size
    if (!isMapSizeAllowed(map)) {
      viewer.sendWarning(
          text()
              .append(map.getStyledName(MapNameStyle.COLOR))
              .append(text(" does not fit the online player count"))
              .append(newline())
              .append(text("Please request a different map"))
              .build());
      return;
    }

    // Don't allow more than one map per sponsor
    if (isQueued(player.getUniqueId())) {
      viewer.sendWarning(text("You already have a map in the sponsor queue"));
      return;
    }

    // Only allow a certain number of sponsors per the queue
    if (!isQueueOpen()) {
      viewer.sendWarning(text("The sponsor queue is full! Please submit your request later"));
      return;
    }

    getRequestProfile(player.getUniqueId())
        .thenAcceptAsync(
            profile -> {
              if (!canSponsor(player.getUniqueId())) {
                viewer.sendWarning(
                    getCooldownMessage(
                        profile.getLastSponsorTime(), getRequestConfig().getSponsorCooldown()));
                return;
              }

              // Check tokens
              if (profile.getSponsorTokens() < 1) {
                viewer.sendWarning(text("You don't have enough sponsor tokens"));
                return;
              }

              // Sponsor Queue
              // -> Add to queue, don't charge token until sponsor is processed
              queueRequest(player, map);

              // Send confirmation, including map queue position
              viewer.sendMessage(
                  text()
                      .append(SPONSOR)
                      .append(text(" You've sponsored ", NamedTextColor.YELLOW))
                      .append(map.getStyledName(MapNameStyle.COLOR)));
              if (sponsors.size() > 1) {
                viewer.sendMessage(
                    text()
                        .append(text("Queue position "))
                        .append(text("#" + sponsors.size(), NamedTextColor.YELLOW))
                        .append(text(" Use "))
                        .append(text("/queue", NamedTextColor.AQUA))
                        .append(text(" to track status"))
                        .color(NamedTextColor.GRAY)
                        .clickEvent(ClickEvent.runCommand("/sponsor queue"))
                        .hoverEvent(
                            showText(text("Click to view queue status", NamedTextColor.GRAY)))
                        .build());
              } else {
                viewer.sendMessage(
                    text("Request will be added to the next map vote", NamedTextColor.GRAY));
              }
            });
  }

  @Override
  public boolean canRequest(UUID playerId) {
    return cooldown.getIfPresent(playerId) == null;
  }

  @Override
  public boolean canSponsor(UUID playerId) {
    RequestProfile profile = getCached(playerId);
    if (profile != null) {
      if (profile.getLastSponsorTime() == null) return true;
      Duration timeSince = Duration.between(profile.getLastSponsorTime(), Instant.now());
      return getRequestConfig().getSponsorCooldown().minus(timeSince).isNegative();
    }
    return false;
  }

  @Override
  public Set<UUID> getRequesters(MapInfo map) {
    return requests.asMap().entrySet().stream()
        .filter(e -> e.getValue().equals(map))
        .map(Entry::getKey)
        .collect(Collectors.toSet());
  }

  @Override
  public int clearRequests(MapInfo map) {
    Set<UUID> requesters = getRequesters(map);
    requesters.forEach(requests::invalidate);
    return requesters.size();
  }

  @Override
  public void clearAllRequests() {
    requests.invalidateAll();
  }

  @Override
  public boolean cancelSponsorRequest(UUID playerId) {
    return this.sponsors.removeIf(s -> s.getPlayerId().equals(playerId));
  }

  @Override
  public Optional<SponsorRequest> getPendingSponsor(UUID playerId) {
    return this.sponsors.stream().filter(sr -> sr.getPlayerId().equals(playerId)).findAny();
  }

  @Override
  public int queueIndex(SponsorRequest request) {
    return sponsors.indexOf(request);
  }

  @Override
  public boolean hasMapCooldown(MapInfo map) {
    MapCooldown cooldown = mapCooldown.get(map);
    if (cooldown == null) return false;

    if (cooldown.hasExpired()) {
      mapCooldown.remove(map);
      return false;
    }

    return true;
  }

  @Override
  public Map<MapInfo, MapCooldown> getMapCooldowns() {
    return mapCooldown;
  }

  private Component getCooldownMessage(Instant lastRequest, Duration cooldownTime) {
    Duration timeLeft = cooldownTime.minus(Duration.between(lastRequest, Instant.now()));

    return text()
        .append(text("Please wait "))
        .append(duration(timeLeft, NamedTextColor.YELLOW))
        .append(text(" before submitting another request"))
        .build();
  }

  private boolean hasCooldown(Player player, Audience viewer) {
    boolean canRequest = canRequest(player.getUniqueId());

    if (!canRequest) {
      Instant lastRequestTime = cooldown.getIfPresent(player.getUniqueId());
      if (lastRequestTime != null) {
        viewer.sendWarning(getCooldownMessage(lastRequestTime, getRequestConfig().getCooldown()));
      }
    }
    return !canRequest;
  }

  private boolean canRefund(Player player) {
    return getRequestConfig().isRefunded()
        && player.hasPermission(CommunityPermissions.REQUEST_REFUND);
  }

  private Component getRequestMessage(Player player, MapInfo map) {
    TextComponent.Builder message =
        text().append(text("Requested ")).append(map.getStyledName(MapNameStyle.COLOR));

    if (player.hasPermission(CommunityPermissions.REQUEST_SPONSOR)
        && canSponsor(player.getUniqueId())
        && isMapSizeAllowed(map)) {
      message.append(
          text()
              .append(space())
              .append(SPONSOR)
              .clickEvent(ClickEvent.runCommand("/sponsor request" + map.getName()))
              .hoverEvent(
                  HoverEvent.showText(
                      text()
                          .append(text("Click to sponsor this request", NamedTextColor.GRAY))
                          .append(newline())
                          .append(
                              text(
                                  "Map will queue and be auto-added to the vote when avaiable",
                                  NamedTextColor.YELLOW)))));
    }

    return message.color(NamedTextColor.GRAY).build();
  }

  private Component getReplaceMessage(MapInfo oldMap, MapInfo newMap) {
    return text()
        .append(text("Replaced request for "))
        .append(oldMap.getStyledName(MapNameStyle.COLOR))
        .append(text(" with "))
        .append(newMap.getStyledName(MapNameStyle.COLOR))
        .color(NamedTextColor.GRAY)
        .build();
  }

  private Component getTokenRefreshMessage(int amount, int total, boolean daily) {
    return text()
        .append(TOKEN)
        .append(text(" Recieved "))
        .append(text("+" + amount, NamedTextColor.GREEN, TextDecoration.BOLD))
        .append(text(" sponsor token" + (amount != 1 ? "s" : "")))
        .append(text(" ("))
        .append(text("Total: ", NamedTextColor.GRAY))
        .append(text(total, NamedTextColor.YELLOW, TextDecoration.BOLD))
        .append(text(")"))
        .color(NamedTextColor.GOLD)
        .hoverEvent(
            HoverEvent.showText(
                text("Next token refresh will be in ", NamedTextColor.GRAY)
                    .append(duration(Duration.ofDays(daily ? 1 : 7), NamedTextColor.YELLOW))))
        .build();
  }

  private void sendDelayedTokenRefreshMessage(Player player, int amount, boolean daily, int total) {
    Community.get()
        .getServer()
        .getScheduler()
        .runTaskLater(
            Community.get(),
            () -> {
              Audience viewer = Audience.get(player);
              viewer.sendMessage(getTokenRefreshMessage(amount, total, daily));
              viewer.sendMessage(
                  text()
                      .append(text("Spend tokens by using ", NamedTextColor.GRAY))
                      .append(text("/sponsor", NamedTextColor.YELLOW)));
              viewer.playSound(Sounds.GET_TOKENS);
            },
            20L * 3);
  }

  private void alertStaff(Player player, MapInfo map, boolean sponsor) {
    Component alert =
        text()
            .append(player(player, NameStyle.FANCY))
            .append(text(" has "))
            .append(text(sponsor ? "sponsored " : "requested "))
            .append(map.getStyledName(MapNameStyle.COLOR))
            .color(NamedTextColor.YELLOW)
            .build();

    BroadcastUtils.sendAdminChatMessage(alert);
  }

  private void queueRequest(Player player, MapInfo map) {
    this.sponsors.add(new SponsorRequest(player.getUniqueId(), map, canRefund(player)));
    Community.log(
        "%s has queued a map (%s) (refund: %s) - Total Queued == %d",
        player.getName(), map.getName(), canRefund(player), sponsors.size());
    alertStaff(player, map, true);
  }

  private boolean isQueued(UUID playerId) {
    return sponsors.stream().anyMatch(sr -> sr.getPlayerId().equals(playerId));
  }

  private boolean isQueueOpen() {
    return sponsors.size() < getRequestConfig().getMaxQueue();
  }

  @Nullable
  private VotingPool getVotingPool() {
    MapOrder order = PGM.get().getMapOrder();
    if (order instanceof MapPoolManager) {
      MapPoolManager manager = (MapPoolManager) order;
      if (manager.getActiveMapPool() instanceof VotingPool) {
        VotingPool votePool = (VotingPool) manager.getActiveMapPool();
        if (votePool.getCurrentPoll() == null) {
          return votePool;
        }
      }
    }
    return null;
  }

  private void startNewMapCooldown(MapInfo map, Duration matchLength) {
    this.mapCooldown.putIfAbsent(
        map,
        new MapCooldown(
            Instant.now(), matchLength.multipliedBy(getRequestConfig().getMapCooldownMultiply())));
  }
}

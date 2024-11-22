package dev.pgm.community.requests.feature;

import static dev.pgm.community.utils.MessageUtils.formatTokenTransaction;
import static dev.pgm.community.utils.PGMUtils.getCurrentMap;
import static dev.pgm.community.utils.PGMUtils.isBlitz;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.event.HoverEvent.showText;
import static tc.oc.pgm.util.player.PlayerComponent.player;
import static tc.oc.pgm.util.text.TemporalComponent.duration;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dev.pgm.community.Community;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.feature.FeatureBase;
import dev.pgm.community.party.MapParty;
import dev.pgm.community.requests.MapCooldown;
import dev.pgm.community.requests.RequestConfig;
import dev.pgm.community.requests.RequestProfile;
import dev.pgm.community.requests.SponsorRequest;
import dev.pgm.community.requests.menu.SponsorMenu;
import dev.pgm.community.requests.supervotes.SuperVoteManager;
import dev.pgm.community.users.feature.UsersFeature;
import dev.pgm.community.utils.BroadcastUtils;
import dev.pgm.community.utils.MessageUtils;
import dev.pgm.community.utils.PGMUtils;
import dev.pgm.community.utils.PGMUtils.MapSizeBounds;
import dev.pgm.community.utils.Sounds;
import dev.pgm.community.utils.VisibilityUtils;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
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
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapOrder;
import tc.oc.pgm.api.map.Phase;
import tc.oc.pgm.api.map.VariantInfo;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.match.event.MatchVoteFinishEvent;
import tc.oc.pgm.events.PlayerJoinMatchEvent;
import tc.oc.pgm.restart.RestartManager;
import tc.oc.pgm.rotation.MapPoolManager;
import tc.oc.pgm.rotation.vote.MapPoll;
import tc.oc.pgm.rotation.vote.VotePoolOptions;
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
  private SponsorVotingBookCreator bookCreator;

  private SuperVoteManager superVotes;

  private boolean accepting;

  public RequestFeatureBase(
      RequestConfig config, Logger logger, String featureName, UsersFeature users) {
    super(config, logger, "Requests (" + featureName + ")");
    this.requests =
        CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.HOURS).build();
    this.cooldown = CacheBuilder.newBuilder()
        .expireAfterWrite(config.getCooldown().getSeconds(), TimeUnit.SECONDS)
        .build();
    this.mapCooldown = Maps.newHashMap();
    this.sponsors = Lists.newLinkedList();
    this.currentSponsor = null;
    this.bookCreator = new SponsorVotingBookCreator(this);
    this.superVotes = new SuperVoteManager(config, logger);

    if (getConfig().isEnabled() && PGMUtils.isPGMEnabled()) {
      enable();
      MapPoll.setVotingBookCreator(bookCreator);
    }
  }

  public RequestConfig getRequestConfig() {
    return (RequestConfig) getConfig();
  }

  @Override
  public void openMenu(Player viewer) {
    SponsorMenu menu = new SponsorMenu(getAvailableSponsorMaps(), viewer);
    menu.getInventory().open(viewer);
  }

  @Override
  public List<MapInfo> getAvailableSponsorMaps() {
    return Lists.newArrayList(PGM.get().getMapLibrary().getMaps()).stream()
        .filter(this::isMapSizeAllowed)
        .filter(m -> m.getPhase() == Phase.PRODUCTION)
        .filter(m -> !hasMapCooldown(m))
        .collect(Collectors.toList());
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
    final Player player = event.getPlayer();

    superVotes.onRelogin(player);

    onLogin(event).thenAcceptAsync(profile -> {
      if (player.hasPermission(CommunityPermissions.REQUEST_SPONSOR)) {
        int refresh = 0;
        boolean daily = false;

        // Check permission to determine what amount to refresh
        // Always prefer the daily compared to the weekly (e.g sponsor inherits donor perms,
        // only give daily not weekly)
        if (player.hasPermission(CommunityPermissions.TOKEN_DAILY)) {
          if (profile.hasDayElapsed()) {
            refresh = getRequestConfig().getDailyTokenAmount();
            daily = true;
          }
        } else if (player.hasPermission(CommunityPermissions.TOKEN_WEEKLY)) {
          if (profile.hasWeekElapsed()) {
            refresh = getRequestConfig().getWeeklyTokenAmount();
          }
        }

        // Refresh token amount as long as they have less than the max
        if (refresh > 0 && profile.getSponsorTokens() < getRequestConfig().getMaxTokens()) {
          profile.refreshTokens(refresh); // Set new token amount
          update(profile); // Save new token balance to database
          sendDelayedTokenRefreshMessage(player, refresh, daily, profile.getSponsorTokens());
        }
      }
    });
  }

  private SponsorRequest getNextSponsor() {
    Queue<SponsorRequest> requests = new LinkedList<>();
    SponsorRequest validRequest = null;

    while (!sponsors.isEmpty()) {
      SponsorRequest request = sponsors.poll();
      if (isMapSizeAllowed(request.getMap())) {
        validRequest = request;
        break;
      } else {
        requests.offer(request);
        sendWrongSizeMapError(request);
      }
    }

    while (!requests.isEmpty()) {
      sponsors.offer(requests.poll());
    }

    return validRequest;
  }

  @EventHandler
  public void onMatchEnd(MatchFinishEvent event) {
    superVotes.onVoteStart();

    if (currentSponsor != null) { // Reset current sponsor after match ends
      currentSponsor = null;
    }

    // Add cooldown for existing map and all variants
    startNewMapCooldown(event.getMatch().getMap(), event.getMatch().getDuration());

    MapPoolManager poolManager = getPoolManager();
    if (poolManager == null) return; // Cancel if pool manager not found

    VotePoolOptions options = poolManager.getVoteOptions();

    if (sponsors.isEmpty()) return;
    if (!options.canAddMap()) return;
    if (poolManager.getOverriderMap() != null) return;

    SponsorRequest nextRequest = getNextSponsor();

    if (nextRequest != null) {
      // Notify PGM of sponsored map
      options.addMap(nextRequest.getMap(), nextRequest.getPlayerId());

      // Track the current sponsor
      this.currentSponsor = nextRequest;

      // Update profile
      getRequestProfile(nextRequest.getPlayerId()).thenAcceptAsync(profile -> {
        // Update RequestProfile with sponsor map info
        profile.sponsor(nextRequest.getMap());
        update(profile);
      });

      // Alert online player if their sponsor request has been processed
      Player requester = Bukkit.getPlayer(nextRequest.getPlayerId());
      if (requester != null) {
        Audience player = Audience.get(requester);
        player.sendMessage(formatTokenTransaction(
            -1,
            text(
                "Your sponsored map has been added to the vote!",
                NamedTextColor.GREEN,
                TextDecoration.BOLD),
            canRefund(requester)
                ? text("If your map wins the vote, you'll get your token back", NamedTextColor.GRAY)
                : null));
        player.playSound(Sounds.SPEND_TOKENS);
      }
    }
  }

  @EventHandler
  public void onVoteEnd(MatchVoteFinishEvent event) {
    superVotes.onVoteEnd();

    if (currentSponsor != null) {
      Player player = Bukkit.getPlayer(currentSponsor.getPlayerId());

      // Same map = winner, refund the token even if offline
      if (currentSponsor.getMap().equals(event.getPickedMap()) && currentSponsor.canRefund()) {
        getRequestProfile(currentSponsor.getPlayerId()).thenAcceptAsync(profile -> {
          profile.giveSponsorToken(1);
          update(profile);

          if (player != null) {
            Audience viewer = Audience.get(player);
            viewer.sendMessage(formatTokenTransaction(
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

  @EventHandler(priority = EventPriority.LOW)
  public void onMatchJoinMessage(PlayerJoinMatchEvent event) {
    MapInfo map = event.getMatch().getMap();

    if (getCurrentSponsor() == null) return;
    if (VisibilityUtils.isDisguised(getCurrentSponsor().getPlayerId())) return;
    if (!getCurrentSponsor().getMap().equals(map)) return;

    event.getExtraLines().add(empty());
    event
        .getExtraLines()
        .add(text()
            .append(text(" Sponsored by "))
            .append(player(getCurrentSponsor().getPlayerId(), NameStyle.FANCY))
            .color(NamedTextColor.GRAY)
            .build());
  }

  private Cache<UUID, String> voteConfirm =
      CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.SECONDS).build();

  private static final List<String> BLOCKED_COMMANDS =
      List.of("/vote add", "/pgm:vote add", "/sn", "/setnext", "/pgm:sn", "/pgm:setnext");

  public static boolean isBlockedCommand(String command) {
    return BLOCKED_COMMANDS.stream().anyMatch(command::startsWith);
  }

  @EventHandler
  public void onVoteAddCommand(PlayerCommandPreprocessEvent event) {
    if (!isBlockedCommand(event.getMessage())) return;
    if (sponsors.isEmpty()) return;
    if (voteConfirm.getIfPresent(event.getPlayer().getUniqueId()) != null) return;
    event.setCancelled(true);
    voteConfirm.put(event.getPlayer().getUniqueId(), "");
    Audience viewer = Audience.get(event.getPlayer());
    viewer.sendWarning(text("A sponsor map has already been added to the vote!"));
    viewer.sendWarning(text("If you still want to adjust the vote, click ", NamedTextColor.GRAY)
        .append(text()
            .append(text("[", NamedTextColor.GRAY))
            .append(text("here", NamedTextColor.YELLOW))
            .append(text("]", NamedTextColor.GRAY)))
        .clickEvent(ClickEvent.runCommand(event.getMessage()))
        .hoverEvent(HoverEvent.showText(text("Click to run command again", NamedTextColor.GRAY))));
  }

  @Override
  public void request(Player player, MapInfo map) {
    Audience viewer = Audience.get(player);

    // Check if enabled
    if (!isAccepting()) {
      viewer.sendWarning(text("Sorry, map requests are not being accepted at this time"));
      return;
    }

    // Don't allow developmental maps
    if (map.getPhase() == Phase.DEVELOPMENT) {
      viewer.sendWarning(text()
          .append(map.getStyledName(MapNameStyle.COLOR))
          .append(text(" may not be requested"))
          .build());
      return;
    }

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
    getRequestProfile(player.getUniqueId()).thenAcceptAsync(profile -> {
      profile.request(map);
      update(profile);
    });

    // Alert the staff
    alertStaff(player, map, false);
  }

  @Override
  public void sponsor(Player player, MapInfo map) {
    Audience viewer = Audience.get(player);

    // Disallow current map from being selected
    if (getCurrentMap() != null && getCurrentMap().equals(map)) {
      viewer.sendWarning(text("Please select a different map"));
      return;
    }

    // Disallow Sponsor during map party
    if (isPartyActive()) {
      viewer.sendWarning(text("Sorry, sponsoring is disabled during the party."));
      return;
    }

    // Disallow Sponsor when a server restart is queued
    if (isRestartQueued()) {
      viewer.sendWarning(
          text("Server will restart after this match. Please submit your request afterward."));
      return;
    }

    // Don't allow developmental maps
    if (map.getPhase() != Phase.PRODUCTION) {
      viewer.sendWarning(text()
          .append(map.getStyledName(MapNameStyle.COLOR))
          .append(text(" may not be selected."))
          .build());
      return;
    }

    // Check if map is already queued
    if (isMapQueued(map)) {
      viewer.sendWarning(text()
          .append(map.getStyledName(MapNameStyle.COLOR))
          .append(text(" is already in the queue!"))
          .build());
      return;
    }

    // Check if map has a cooldown
    if (hasMapCooldown(map)) {
      MapCooldown cooldown = getMapCooldown(map);
      viewer.sendWarning(text()
          .append(text("This map can be sponsored in ", NamedTextColor.RED))
          .append(TemporalComponent.duration(cooldown.getTimeRemaining(), NamedTextColor.YELLOW))
          .build());
      return;
    }

    // Check map size
    if (!isMapSizeAllowed(map)) {
      viewer.sendWarning(text()
          .append(map.getStyledName(MapNameStyle.COLOR)
              .hoverEvent(HoverEvent.showText(text()
                  .append(text("Max of ", NamedTextColor.GRAY))
                  .append(text(PGMUtils.getMapMaxSize(map), NamedTextColor.RED)))))
          .append(text(" does not fit the online player count "))
          .append(getMapSizeBoundsComponent())
          .append(newline())
          .append(text("Please request a different map!"))
          .build());
      return;
    }

    // Don't allow more than one map per sponsor
    if (isQueued(player.getUniqueId())) {
      viewer.sendWarning(text("You already have a map in the sponsor queue."));
      return;
    }

    // Only allow a certain number of sponsors per the queue
    if (!isQueueOpen()) {
      viewer.sendWarning(text("The sponsor queue is full! Please submit your request later."));
      return;
    }

    getRequestProfile(player.getUniqueId()).thenAcceptAsync(profile -> {
      if (!canSponsor(player)) {
        viewer.sendWarning(getCooldownMessage(
            profile.getLastSponsorTime(), getRequestConfig().getSponsorCooldown(player)));
        return;
      }

      // Check tokens
      if (profile.getSponsorTokens() < 1) {
        viewer.sendWarning(text("You don't have enough sponsor tokens!"));
        return;
      }

      // Sponsor Queue
      // -> Add to queue, don't charge token until sponsor is processed
      queueRequest(player, map);

      // Send confirmation, including map queue position
      viewer.sendMessage(text()
          .append(SPONSOR)
          .append(text(" You've sponsored ", NamedTextColor.YELLOW))
          .append(map.getStyledName(MapNameStyle.COLOR)));
      if (sponsors.size() > 1) {
        viewer.sendMessage(text()
            .append(text("Queue position "))
            .append(text("#" + sponsors.size(), NamedTextColor.YELLOW))
            .append(text(" Use "))
            .append(text("/sponsor queue", NamedTextColor.AQUA))
            .append(text(" to track status"))
            .color(NamedTextColor.GRAY)
            .clickEvent(ClickEvent.runCommand("/sponsor queue"))
            .hoverEvent(showText(text("Click to view queue status", NamedTextColor.GRAY)))
            .build());
      } else {
        if (isBlitz()) {
          viewer.sendMessage(
              text("Request will be added to the next non-blitz map vote.", NamedTextColor.GRAY));
        } else {
          viewer.sendMessage(
              text("Request will be added to the next map vote.", NamedTextColor.GRAY));
        }
      }
    });
  }

  @Override
  public void superVote(Player player) {
    Audience viewer = Audience.get(player);

    if (!getRequestConfig().isSuperVoteEnabled()) {
      viewer.sendWarning(text("Super votes are not enabled!"));
      return;
    }

    if (!canSuperVote(player)) {
      viewer.sendWarning(text("You have already activated a super vote for this vote!"));
      return;
    }

    if (!superVotes.isVotingActive()) {
      viewer.sendWarning(text("You can only activate a super vote after the match ends!"));
      return;
    }

    getRequestProfile(player.getUniqueId()).thenAcceptAsync(profile -> {
      if (profile.getSuperVotes() < 1) {
        viewer.sendWarning(text("You don't have enough super votes!"));
        return;
      }

      // Save profile
      profile.superVote();
      update(profile);

      // Activate permissions
      superVotes.onActivate(player);

      // Broadcast or send message
      sendSuperVoterActivationFeedback(player);
    });

    // Re-Open the vote book
    Bukkit.dispatchCommand(player, "votebook");
  }

  @Override
  public boolean canRequest(UUID playerId) {
    return cooldown.getIfPresent(playerId) == null;
  }

  @Override
  public boolean canSponsor(Player player) {
    RequestProfile profile = getCached(player.getUniqueId());
    if (profile != null) {
      if (profile.getLastSponsorTime() == null) return true;
      Duration timeSince = Duration.between(profile.getLastSponsorTime(), Instant.now());
      return getRequestConfig().getSponsorCooldown(player).minus(timeSince).isNegative();
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
  public boolean canSuperVote(Player player) {
    return !isSuperVoteActive(player);
  }

  @Override
  public boolean isSuperVoteActive(Player player) {
    return superVotes.isActive(player);
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
  public boolean isAccepting() {
    return accepting;
  }

  @Override
  public void toggleAccepting() {
    this.accepting = !accepting;
  }

  @Override
  public boolean cancelSponsorRequest(UUID playerId) {
    return this.sponsors.removeIf(s -> s.getPlayerId().equals(playerId));
  }

  @Override
  public Optional<SponsorRequest> getPendingSponsor(UUID playerId) {
    return this.sponsors.stream()
        .filter(sr -> sr.getPlayerId().equals(playerId))
        .findAny();
  }

  @Override
  public int queueIndex(SponsorRequest request) {
    return sponsors.indexOf(request);
  }

  @Override
  public MapSizeBounds getCurrentMapSizeBounds() {
    return PGMUtils.getMapSizeBounds(
        getRequestConfig().getLowerLimitOffset(),
        getRequestConfig().getUpperLimitOffset(),
        getRequestConfig().getScaleFactor());
  }

  private boolean isACooldownVariant(MapInfo map) {
    return mapCooldown.entrySet().stream().anyMatch((entry) -> {
      MapInfo otherMap = entry.getKey();
      MapCooldown cooldown = entry.getValue();

      if (cooldown.hasExpired()) return false;

      for (VariantInfo variant : otherMap.getVariants().values()) {
        if (variant.getId().equalsIgnoreCase(map.getId())) {
          return true;
        }
      }

      return false;
    });
  }

  private MapCooldown getMapCooldown(MapInfo map) {
    if (mapCooldown.containsKey(map)) {
      return mapCooldown.get(map);
    }

    for (Map.Entry<MapInfo, MapCooldown> entry : mapCooldown.entrySet()) {
      MapInfo otherMap = entry.getKey();
      MapCooldown cooldown = entry.getValue();

      for (VariantInfo variant : otherMap.getVariants().values()) {
        if (variant.getId().equalsIgnoreCase(map.getId())) {
          return cooldown;
        }
      }
    }

    return null;
  }

  @Override
  public boolean hasMapCooldown(MapInfo map) {
    if (isACooldownVariant(map)) return true;

    MapCooldown cooldown = getMapCooldown(map);
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

  @Override
  public int getStandardExtraVoteLevel(Player player) {
    return superVotes.getVoteLevel(player);
  }

  @Override
  public int getMultipliedExtraVoteLevel(Player player) {
    return superVotes.getMultipliedVoteLevel(player);
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
        viewer.sendWarning(
            getCooldownMessage(lastRequestTime, getRequestConfig().getCooldown()));
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
        && canSponsor(player)
        && isMapSizeAllowed(map)) {
      message.append(text()
          .append(space())
          .append(SPONSOR)
          .clickEvent(ClickEvent.runCommand("/sponsor request" + map.getName()))
          .hoverEvent(HoverEvent.showText(text()
              .append(text("Click to sponsor this request", NamedTextColor.GRAY))
              .append(newline())
              .append(text(
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
        .hoverEvent(HoverEvent.showText(text("Next token refresh will be in ", NamedTextColor.GRAY)
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
              viewer.sendMessage(text()
                  .append(text("Spend tokens by using ", NamedTextColor.GRAY))
                  .append(text("/sponsor", NamedTextColor.YELLOW)));
              viewer.playSound(Sounds.GET_TOKENS);
            },
            20L * 3);
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

  private boolean isMapQueued(MapInfo map) {
    return sponsors.stream().anyMatch(sr -> sr.getMap().equals(map));
  }

  private boolean isQueueOpen() {
    return sponsors.size() < getRequestConfig().getMaxQueue();
  }

  @Nullable
  private MapPoolManager getPoolManager() {
    MapOrder order = PGM.get().getMapOrder();
    if (order instanceof MapPoolManager) {
      return (MapPoolManager) order;
    }
    return null;
  }

  private void startNewMapCooldown(MapInfo map, Duration matchLength) {
    this.mapCooldown.putIfAbsent(
        map,
        new MapCooldown(
            Instant.now(), matchLength.multipliedBy(getRequestConfig().getMapCooldownMultiply())));
  }

  private boolean isPartyActive() {
    if (!Community.get().getFeatures().getParty().isEnabled()) return false;
    MapParty party = Community.get().getFeatures().getParty().getParty();
    return party != null && party.isSetup() && party.isRunning();
  }

  private boolean isMapSizeAllowed(MapInfo map) {
    return PGMUtils.isMapSizeAllowed(
        map,
        getRequestConfig().getLowerLimitOffset(),
        getRequestConfig().getUpperLimitOffset(),
        getRequestConfig().getScaleFactor());
  }

  private void sendWrongSizeMapError(SponsorRequest request) {
    Player player = Bukkit.getPlayer(request.getPlayerId());
    if (player == null || !player.isOnline()) return;
    Audience viewer = Audience.get(player);

    Component remove = text()
        .append(text("[", NamedTextColor.GRAY))
        .append(text("Remove", NamedTextColor.YELLOW))
        .append(text("]", NamedTextColor.GRAY))
        .hoverEvent(HoverEvent.showText(
            text("Click to remove map from sponsor queue", NamedTextColor.GRAY)))
        .clickEvent(ClickEvent.runCommand("/sponsor cancel"))
        .build();

    viewer.sendWarning(text()
        .append(request.getMap().getStyledName(MapNameStyle.COLOR))
        .append(text(" no longer fits the online player count "))
        .append(getMapSizeBoundsComponent())
        .append(text(". We'll try again after the next match, or you can "))
        .append(remove)
        .append(text(" and select a new map to sponsor.")));
  }

  private Component getMapSizeBoundsComponent() {
    MapSizeBounds bounds = getCurrentMapSizeBounds();
    int min = bounds.getLowerBound();
    int max = bounds.getUpperBound();
    return text()
        .append(text("(", NamedTextColor.GRAY))
        .append(text(min, NamedTextColor.GOLD))
        .append(text("-", NamedTextColor.GRAY))
        .append(text(max, NamedTextColor.GOLD))
        .append(text(")", NamedTextColor.GRAY))
        .build();
  }

  private boolean isRestartQueued() {
    return RestartManager.isQueued();
  }

  private void sendSuperVoterActivationFeedback(Player player) {
    Audience viewer = Audience.get(player);
    if (getRequestConfig().isSuperVoteBroadcast()) {
      Component alert = text()
          .append(MessageUtils.VOTE)
          .appendSpace()
          .append(player(player, NameStyle.FANCY))
          .append(text(" has activated a ", NamedTextColor.YELLOW))
          .append(text("super vote", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
          .build();

      BroadcastUtils.sendGlobalMessage(alert);
    } else {
      viewer.sendMessage(text()
          .append(MessageUtils.VOTE)
          .appendSpace()
          .append(text("You activated a ", NamedTextColor.YELLOW))
          .append(text("super vote", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
          .append(text("!", NamedTextColor.YELLOW)));
    }
  }
}

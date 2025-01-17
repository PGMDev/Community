package dev.pgm.community.requests.feature;

import static dev.pgm.community.utils.MessageUtils.formatTokenTransaction;
import static dev.pgm.community.utils.PGMUtils.getCurrentMap;
import static dev.pgm.community.utils.PGMUtils.isBlitz;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static tc.oc.pgm.util.player.PlayerComponent.player;
import static tc.oc.pgm.util.text.TemporalComponent.duration;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;
import dev.pgm.community.Community;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.feature.FeatureBase;
import dev.pgm.community.party.MapParty;
import dev.pgm.community.requests.RequestConfig;
import dev.pgm.community.requests.RequestProfile;
import dev.pgm.community.requests.SponsorRequest;
import dev.pgm.community.requests.menu.SponsorMenu;
import dev.pgm.community.requests.sponsor.SponsorComponents;
import dev.pgm.community.requests.sponsor.SponsorManager;
import dev.pgm.community.requests.supervotes.SuperVoteManager;
import dev.pgm.community.users.feature.UsersFeature;
import dev.pgm.community.utils.BroadcastUtils;
import dev.pgm.community.utils.PGMUtils;
import dev.pgm.community.utils.PGMUtils.MapSizeBounds;
import dev.pgm.community.utils.Sounds;
import dev.pgm.community.utils.VisibilityUtils;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;
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
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.match.event.MatchVoteFinishEvent;
import tc.oc.pgm.events.PlayerJoinMatchEvent;
import tc.oc.pgm.restart.RestartManager;
import tc.oc.pgm.rotation.MapPoolManager;
import tc.oc.pgm.rotation.pools.VotingPool;
import tc.oc.pgm.rotation.vote.MapPoll;
import tc.oc.pgm.rotation.vote.VotePoolOptions;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.TimeUtils;
import tc.oc.pgm.util.named.MapNameStyle;
import tc.oc.pgm.util.named.NameStyle;

public abstract class RequestFeatureBase extends FeatureBase implements RequestFeature {

  // Multiplier for minimum score to allow sponsoring
  private static final double MIN_SCORE_MUL = 0.35;

  private Cache<UUID, MapInfo> requests;

  private Cache<UUID, Instant> cooldown;

  private SponsorVotingBookCreator bookCreator;

  private SponsorManager sponsor;

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
    this.bookCreator = new SponsorVotingBookCreator(this);
    this.sponsor = new SponsorManager(this, config);
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
    BroadcastUtils.sendAdminChatMessage(
        getRequestAdminChatAlert(player, map), CommunityPermissions.REQUEST_STAFF);
  }

  @Override
  public void sponsor(Player player, MapInfo map) {
    Audience viewer = Audience.get(player);

    if (!getRequestConfig().isSponsorEnabled()) {
      viewer.sendWarning(text("Sponsor is not enabled!"));
      return;
    }

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
    if (sponsor.isMapQueued(map)) {
      viewer.sendWarning(text()
          .append(map.getStyledName(MapNameStyle.COLOR))
          .append(text(" is already in the queue!"))
          .build());
      return;
    }

    // Check if map has a cooldown
    var cd = getApproximateCooldown(map);
    if (cd.isPositive()) {
      Component cooldownWarning = cd.toSeconds() > 0
          ? text("This map can be sponsored in ").append(duration(cd, NamedTextColor.YELLOW))
          : text("This map played or lost a vote too recently!");
      viewer.sendWarning(cooldownWarning);
      return;
    }

    // Check map size
    if (!sponsor.isMapSizeAllowed(map)) {
      viewer.sendWarning(
          SponsorComponents.getMapSelectionSizeWarning(map, getCurrentMapSizeBounds()));
      return;
    }

    // Don't allow more than one map per sponsor
    if (sponsor.isQueued(player.getUniqueId())) {
      viewer.sendWarning(text("You already have a map in the sponsor queue."));
      return;
    }

    // Only allow a certain number of sponsors per the queue
    if (!sponsor.isQueueOpen()) {
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
      sponsor.queueSponsorRequest(player, map);

      // Send confirmation, including map queue position
      viewer.sendMessage(SponsorComponents.getSuccessfulSponsorMessage(map));

      if (sponsor.getSponsorQueue().size() > 1) {
        viewer.sendMessage(
            SponsorComponents.getQueuePositionMessage(sponsor.getSponsorQueue().size()));
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
      superVotes.sendSuperVoterActivationFeedback(player);
    });

    // Re-Open the vote book
    Bukkit.dispatchCommand(player, "votebook");
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    final Player player = event.getPlayer();

    superVotes.onRelogin(player);

    onLogin(event).thenAcceptAsync(profile -> {
      if (player.hasPermission(CommunityPermissions.REQUEST_SPONSOR)) {
        sponsor.performTokenRefresh(player, profile);
      }
    });
  }

  @EventHandler
  public void onMatchEnd(MatchFinishEvent event) {
    superVotes.onVoteStart();

    if (sponsor.getCurrentSponsor() != null) { // Reset current sponsor after match ends
      sponsor.setCurrentSponsor(null);
    }

    // Add cooldown for existing map and all variants
    sponsor.startNewMapCooldown(event.getMatch().getMap(), event.getMatch().getDuration());

    MapPoolManager poolManager = getPoolManager();
    if (poolManager == null) return; // Cancel if pool manager not found

    VotePoolOptions options = poolManager.getVoteOptions();

    if (sponsor.getSponsorQueue().isEmpty()) return;
    if (!options.canAddMap()) return;
    if (poolManager.getOverriderMap() != null) return;
    if (isBlitz()) return; // Prevent sponsor after blitz map

    SponsorRequest nextRequest = sponsor.getNextSponsor();

    if (nextRequest != null) {
      // Notify PGM of sponsored map
      options.addMap(nextRequest.getMap(), nextRequest.getPlayerId());

      // Track the current sponsor
      sponsor.setCurrentSponsor(nextRequest);

      // Update profile
      getRequestProfile(nextRequest.getPlayerId()).thenAcceptAsync(profile -> {
        // Update RequestProfile with sponsor map info
        profile.sponsor(nextRequest.getMap());
        update(profile);
      });

      // Alert online player if their sponsor request has been processed
      sponsor.alertRequesterToConfirmation(nextRequest.getPlayerId());
    }
  }

  @EventHandler
  public void onVoteEnd(MatchVoteFinishEvent event) {
    superVotes.onVoteEnd();

    SponsorRequest currentSponsor = sponsor.getCurrentSponsor();

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
        .add(SponsorComponents.getSponsoredJoinMessage(getCurrentSponsor().getPlayerId()));
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
    if (sponsor.getSponsorQueue().isEmpty()) return;
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
  public boolean isAccepting() {
    return accepting;
  }

  @Override
  public void toggleAccepting() {
    this.accepting = !accepting;
  }

  @Override
  public MapSizeBounds getCurrentMapSizeBounds() {
    return sponsor.getCurrentMapSizeBounds();
  }

  // --- Sponsor ---

  @Override
  public SponsorRequest getCurrentSponsor() {
    return sponsor.getCurrentSponsor();
  }

  @Override
  public boolean cancelSponsorRequest(UUID playerId) {
    return this.sponsor.getSponsorQueue().removeIf(s -> s.getPlayerId().equals(playerId));
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
  public void openMenu(Player viewer) {
    SponsorMenu menu = new SponsorMenu(getAvailableSponsorMaps(), viewer);
    menu.getInventory().open(viewer);
  }

  @Override
  public List<MapInfo> getAvailableSponsorMaps() {
    return sponsor.getAvailableSponsorMaps();
  }

  @Override
  public Queue<SponsorRequest> getSponsorQueue() {
    return sponsor.getSponsorQueue();
  }

  @Override
  public Optional<SponsorRequest> getPendingSponsor(UUID playerId) {
    return this.sponsor.getSponsorQueue().stream()
        .filter(sr -> sr.getPlayerId().equals(playerId))
        .findAny();
  }

  @Override
  public int queueIndex(SponsorRequest request) {
    return this.sponsor.getSponsorQueue().indexOf(request);
  }

  // --- Requests ---

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
  public boolean canRequest(UUID playerId) {
    return cooldown.getIfPresent(playerId) == null;
  }

  // --- Super Votes ---

  @Override
  public boolean canSuperVote(Player player) {
    return !isSuperVoteActive(player);
  }

  @Override
  public boolean isSuperVoteActive(Player player) {
    return superVotes.isActive(player);
  }

  @Override
  public int getStandardExtraVoteLevel(Player player) {
    return superVotes.getVoteLevel(player);
  }

  @Override
  public int getMultipliedExtraVoteLevel(Player player) {
    return superVotes.getMultipliedVoteLevel(player);
  }

  // --- Components ---

  private Component getCooldownMessage(Instant lastRequest, Duration cooldownTime) {
    Duration timeLeft = cooldownTime.minus(Duration.between(lastRequest, Instant.now()));

    return text()
        .append(text("Please wait "))
        .append(duration(timeLeft, NamedTextColor.YELLOW))
        .append(text(" before submitting another request"))
        .build();
  }

  private Component getRequestMessage(Player player, MapInfo map) {
    TextComponent.Builder message =
        text().append(text("Requested ")).append(map.getStyledName(MapNameStyle.COLOR));

    if (player.hasPermission(CommunityPermissions.REQUEST_SPONSOR)
        && canSponsor(player)
        && sponsor.isMapSizeAllowed(map)) {
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

  private Component getRequestAdminChatAlert(Player player, MapInfo map) {
    return text()
        .append(player(player, NameStyle.FANCY))
        .append(text(" has "))
        .append(text("requested "))
        .append(map.getStyledName(MapNameStyle.COLOR))
        .color(NamedTextColor.YELLOW)
        .build();
  }

  // -- Map Cooldowns ---

  @Override
  public boolean hasMapCooldown(MapInfo map) {
    return getApproximateCooldown(map).isPositive();
  }

  /**
   * How long until the map (or any of its variants) is off cooldown.
   *
   * @param map the map to check
   * @return zero for no cooldown. 1ms for undetermined (ie: score-based cooldown), otherwise the
   *     full cd duration
   */
  @Override
  public Duration getApproximateCooldown(MapInfo map) {
    var mapLibrary = PGM.get().getMapLibrary();
    return map.getVariants().values().stream()
        .map(variant -> mapLibrary.getMapById(variant.getId()))
        .filter(Objects::nonNull)
        .map(m -> TimeUtils.max(sponsor.getSponsorCooldown(m), getPgmCooldown(m)))
        .max(Comparator.naturalOrder())
        .orElse(Duration.ZERO);
  }

  private Duration getPgmCooldown(MapInfo map) {
    if (!getRequestConfig().isPGMCooldownsUsed()) return Duration.ZERO;
    if (!(PGMUtils.getActiveMapPool() instanceof VotingPool pool)) return Duration.ZERO;
    var data = pool.getVoteData(map);
    Duration cd = data.remainingCooldown(pool.constants);
    if (cd.isPositive()) return cd;
    return data.getScore() < (pool.constants.defaultScore() * MIN_SCORE_MUL)
        ? Duration.ofMillis(1L)
        : Duration.ZERO;
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

  // --- Utils ---

  private MapPoolManager getPoolManager() {
    MapOrder order = PGM.get().getMapOrder();
    if (order instanceof MapPoolManager) {
      return (MapPoolManager) order;
    }
    return null;
  }

  private boolean isPartyActive() {
    if (!Community.get().getFeatures().getParty().isEnabled()) return false;
    MapParty party = Community.get().getFeatures().getParty().getParty();
    return party != null && party.isSetup() && party.isRunning();
  }

  private boolean isRestartQueued() {
    return RestartManager.isQueued();
  }
}

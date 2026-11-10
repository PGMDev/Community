package dev.pgm.community.requests.feature.types;

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
import dev.pgm.community.network.feature.NetworkFeature;
import dev.pgm.community.network.subs.types.RequestSubscriber;
import dev.pgm.community.network.subs.types.SponsorClaimSubscriber;
import dev.pgm.community.network.updates.types.RequestUpdate;
import dev.pgm.community.network.updates.types.SponsorClaimUpdate;
import dev.pgm.community.party.MapParty;
import dev.pgm.community.requests.RequestConfig;
import dev.pgm.community.requests.RequestProfile;
import dev.pgm.community.requests.SponsorRequest;
import dev.pgm.community.requests.audit.Currency;
import dev.pgm.community.requests.audit.CurrencyAction;
import dev.pgm.community.requests.audit.CurrencyChange;
import dev.pgm.community.requests.audit.CurrencyHistoryPage;
import dev.pgm.community.requests.audit.CurrencyResult;
import dev.pgm.community.requests.audit.InsufficientCurrencyException;
import dev.pgm.community.requests.audit.PendingRefund;
import dev.pgm.community.requests.audit.RefreshNotEligibleException;
import dev.pgm.community.requests.feature.RequestFeature;
import dev.pgm.community.requests.feature.SponsorVotingBookCreator;
import dev.pgm.community.requests.menu.SponsorMenu;
import dev.pgm.community.requests.sponsor.SponsorComponents;
import dev.pgm.community.requests.sponsor.SponsorManager;
import dev.pgm.community.requests.store.RequestStore;
import dev.pgm.community.requests.supervotes.SuperVoteManager;
import dev.pgm.community.users.feature.UsersFeature;
import dev.pgm.community.utils.BroadcastUtils;
import dev.pgm.community.utils.MessageUtils;
import dev.pgm.community.utils.PGMUtils;
import dev.pgm.community.utils.PGMUtils.MapSizeBounds;
import dev.pgm.community.utils.Sounds;
import dev.pgm.community.utils.VisibilityUtils;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitTask;
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
import tc.oc.pgm.rotation.vote.events.MatchPlayerVoteEvent;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.TimeUtils;
import tc.oc.pgm.util.named.MapNameStyle;
import tc.oc.pgm.util.named.NameStyle;

public class RequestFeatureCore extends FeatureBase implements RequestFeature {

  // Multiplier for minimum score to allow sponsoring
  private static final double MIN_SCORE_MUL = 0.35;

  private final RequestStore store;
  private final Cache<UUID, MapInfo> requests;
  private final Cache<UUID, Instant> cooldown;
  private final SponsorVotingBookCreator bookCreator;
  private final SponsorManager sponsor;
  private final SuperVoteManager superVotes;
  private final UsersFeature users;
  private final NetworkFeature network;

  private final Map<UUID, CompletableFuture<?>> currencyChanges = new HashMap<>();
  private final Map<UUID, PendingRefund> pendingRefunds = new ConcurrentHashMap<>();
  private final Map<UUID, Component> refundMessages = new ConcurrentHashMap<>();
  private final Set<UUID> processingRefunds = ConcurrentHashMap.newKeySet();
  private final Map<UUID, CurrencyChange> sponsorCharges = new HashMap<>();
  private final Set<UUID> processingSuperVotes = ConcurrentHashMap.newKeySet();
  private volatile UUID voteId = UUID.randomUUID();
  private volatile boolean processingSponsor;
  private BukkitTask refundTask;
  private volatile boolean storedRefunds = true;

  private boolean accepting;

  public RequestFeatureCore(
      Configuration config,
      Logger logger,
      UsersFeature users,
      NetworkFeature network,
      RequestStore store) {
    super(new RequestConfig(config), logger, "Requests");
    this.requests =
        CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.HOURS).build();
    this.cooldown = CacheBuilder.newBuilder()
        .expireAfterWrite(getRequestConfig().getCooldown().getSeconds(), TimeUnit.SECONDS)
        .build();
    this.bookCreator = new SponsorVotingBookCreator(this);
    this.sponsor = new SponsorManager(this, getRequestConfig());
    this.superVotes = new SuperVoteManager(getRequestConfig(), logger);
    this.store = store;
    this.users = users;
    this.network = network;

    if (getConfig().isEnabled() && PGMUtils.isPGMEnabled()) {
      enable();
      MapPoll.setVotingBookCreator(bookCreator);
      network.registerSubscriber(new RequestSubscriber(this, network.getNetworkId(), logger));
      network.registerSubscriber(new SponsorClaimSubscriber(this, network.getNetworkId(), logger));
    }
  }

  @Override
  public void enable() {
    super.enable();
    if (isAuditEnabled(Currency.SPONSOR_TOKEN)) {
      store.releaseServerSponsorClaims(getServerId()).exceptionally(failure -> {
        logger.log(Level.WARNING, "Could not clear stale sponsor claims", failure);
        return null;
      });
    }
    if (isAnyAuditEnabled()) {
      store.initializeCurrencyAudit().exceptionally(failure -> {
        logger.log(Level.WARNING, "Currency audit initialization failed", failure);
        return null;
      });
    }
    if (refundTask == null) {
      refundTask =
          Bukkit.getScheduler().runTaskTimer(Community.get(), this::retryRefunds, 600L, 600L);
    }
  }

  @Override
  public void disable() {
    if (refundTask != null) {
      refundTask.cancel();
      refundTask = null;
    }
    super.disable();
  }

  @Override
  public CompletableFuture<Integer> changeCurrency(UUID playerId, CurrencyChange change) {
    return changeCurrency(playerId, change, false);
  }

  private synchronized CompletableFuture<Integer> changeCurrency(
      UUID playerId, CurrencyChange change, boolean forceAudit) {
    boolean auditChange = forceAudit || isAuditEnabled(change.currency());
    CompletableFuture<?> previous =
        currencyChanges.getOrDefault(playerId, CompletableFuture.completedFuture(null));
    CompletableFuture<Integer> next = previous
        .handle((value, failure) -> null)
        .thenCompose(ignored -> getRequestProfile(playerId).thenCompose(profile -> {
          if (profile == null)
            return CompletableFuture.failedFuture(new IllegalStateException("Player not found"));
          if (auditChange || isAuditEnabled(change.currency())) {
            return store
                .changeCurrency(
                    profile,
                    change,
                    getRequestConfig().getMaxTokens(),
                    Community.get().getServerConfig().getServerId())
                .thenApply(result -> {
                  logCurrencyChange(playerId, change, result);
                  RequestProfile cached = store.getCached(playerId);
                  if (cached != null && cached != profile) store.refresh(playerId);
                  if (!result.duplicate()) {
                    network.sendUpdate(new RequestUpdate(playerId));
                  }
                  return result.balanceAfter();
                });
          }
          int balance;
          if (change.currency() == Currency.SUPER_VOTE) {
            if (change.action() == CurrencyAction.SPEND) profile.superVote();
            else profile.giveSuperVotes(change.amount());
            balance = profile.getSuperVotes();
          } else {
            if (change.action().isTokenRefresh()) profile.refreshTokens(change.amount());
            else if (change.action() == CurrencyAction.SPEND) {
              profile.applyCurrency(
                  Currency.SPONSOR_TOKEN,
                  Math.max(0, profile.getSponsorTokens() - 1),
                  profile.getLastTokenRefreshTime(),
                  Instant.now(),
                  change.context());
            } else profile.giveSponsorToken(change.amount());
            balance = profile.getSponsorTokens();
          }
          update(profile);
          return CompletableFuture.completedFuture(balance);
        }));
    currencyChanges.put(playerId, next);
    next.whenComplete((value, failure) -> {
      synchronized (RequestFeatureCore.this) {
        currencyChanges.remove(playerId, next);
      }
      // A low balance is an expected rejection which callers already report to the player
      if (failure != null && InsufficientCurrencyException.isCause(failure)) {
        logAudit(
            change.currency(),
            "&f%s &ccould not spend %s &8- &7not enough balance",
            getAuditName(playerId, change),
            formatAuditAmount(change.currency(), change.amount()));
      } else if (failure != null && RefreshNotEligibleException.isCause(failure)) {
        logAudit(
            change.currency(),
            "&8Skipped %s for &7%s &8- already refreshed or at the token limit",
            change.action().name(),
            getAuditName(playerId, change));
      } else if (failure != null) {
        logger.log(
            Level.WARNING,
            "Currency change failed for " + playerId + " (" + change.eventId() + ")",
            failure);
      }
    });
    return next;
  }

  /** Logs a committed audited change to console, so it can be followed without the database. */
  private void logCurrencyChange(UUID playerId, CurrencyChange change, CurrencyResult result) {
    String name = getAuditName(playerId, change);
    if (result.duplicate()) {
      logAudit(
          change.currency(),
          "&8Skipped %s for &7%s &8- already recorded",
          change.action().name(),
          name);
      return;
    }

    int actual = result.balanceAfter() - result.balanceBefore();
    int shown = actual != 0 ? actual : change.amount();
    // Green when the player gains currency, red when they lose it
    String amount = (shown > 0 ? "&a" : "&c") + formatAuditAmount(change.currency(), shown) + "&7";
    String actor = "&f" + change.actorName() + "&7";
    String description = switch (change.action()) {
      case ADMIN_GRANT -> "was given " + amount + " by " + actor;
      case ADMIN_REMOVE -> "had " + amount + " removed by " + actor;
      case DAILY_REFRESH -> "received " + amount + " from the daily refresh";
      case WEEKLY_REFRESH -> "received " + amount + " from the weekly refresh";
      case SPEND ->
        change.currency() == Currency.SPONSOR_TOKEN
            ? "spent " + amount + " sponsoring &6" + getAuditMapName(change.context())
            : "spent " + amount + " activating a super vote";
      case REFUND ->
        "was refunded " + amount
            + (change.context() != null ? " &8(" + change.context() + ")" : "");
    };

    // Balances never go below zero, so note when less was taken than requested
    String requested = actual != change.amount() ? " &8(requested " + change.amount() + ")" : "";

    logAudit(
        change.currency(),
        "&f%s &7%s%s &8- &7balance &e%d &8-> &e%d",
        name,
        description,
        requested,
        result.balanceBefore(),
        result.balanceAfter());
  }

  /** Writes to console with an audit prefix in place of the usual Community one. */
  private static void logAudit(Currency currency, String format, Object... args) {
    // Prefix colours match each currency's in-game colour
    String prefix = currency == Currency.SPONSOR_TOKEN ? "&e[Token Audit]" : "&5[Super Vote Audit]";
    Bukkit.getConsoleSender()
        .sendRawMessage(ChatColor.translateAlternateColorCodes(
            '&', prefix + " " + String.format(format, args)));
  }

  private String getAuditName(UUID playerId, CurrencyChange change) {
    Player player = Bukkit.getPlayer(playerId);
    if (player != null) return player.getName();
    if (playerId.equals(change.actorId())) return change.actorName();
    String stored = users.getUsername(playerId);
    return stored != null ? stored : playerId.toString();
  }

  private static String formatAuditAmount(Currency currency, int amount) {
    int count = Math.abs(amount);
    String label = currency == Currency.SPONSOR_TOKEN ? "sponsor token" : "super vote";
    return count + " " + label + (count != 1 ? "s" : "");
  }

  private static String getAuditMapName(String mapId) {
    if (mapId == null) return "a map";
    MapInfo map = PGM.get().getMapLibrary().getMap(mapId);
    return map != null ? map.getName() : mapId;
  }

  @Override
  public CompletableFuture<CurrencyHistoryPage> getCurrencyHistory(
      UUID playerId, Currency currency, int page) {
    if (page < 1)
      return CompletableFuture.failedFuture(new IllegalArgumentException("Page must be positive"));
    return store.getCurrencyHistory(playerId, currency, page);
  }

  private void queueRefund(UUID playerId, Currency currency, CurrencyChange spend, String context) {
    queueRefund(playerId, currency, spend, context, null);
  }

  private void queueRefund(
      UUID playerId, Currency currency, CurrencyChange spend, String context, Component message) {
    CurrencyChange refund = new CurrencyChange(
        spend == null
            ? UUID.randomUUID()
            : UUID.nameUUIDFromBytes(
                ("refund:" + spend.eventId()).getBytes(StandardCharsets.UTF_8)),
        currency,
        1,
        CurrencyAction.REFUND,
        null,
        "SYSTEM",
        context,
        spend == null ? null : spend.eventId());
    PendingRefund pending = new PendingRefund(playerId, refund);
    if (message != null) refundMessages.put(refund.eventId(), message);
    pendingRefunds.put(refund.eventId(), pending);
    attemptRefund(pending);
  }

  private void retryRefunds() {
    if (!isAnyAuditEnabled() && pendingRefunds.isEmpty() && !storedRefunds) return;
    store
        .getPendingRefunds()
        .thenAccept(pending -> {
          storedRefunds = !pending.isEmpty();
          pending.forEach(refund -> pendingRefunds.putIfAbsent(refund.change().eventId(), refund));
          pendingRefunds.values().forEach(this::attemptRefund);
        })
        .exceptionally(failure -> {
          // Message only, this retries every 30 seconds and a trace each time would flood the log
          logger.warning("Could not load pending currency refunds: " + failure.getMessage());
          pendingRefunds.values().forEach(this::attemptRefund);
          return null;
        });
  }

  private void attemptRefund(PendingRefund refund) {
    UUID eventId = refund.change().eventId();
    if (!processingRefunds.add(eventId)) return;
    store
        .queueRefund(refund)
        // A queued refund remains audited even if the toggle changes while it is being processed.
        .thenCompose(ignored -> changeCurrency(refund.playerId(), refund.change(), true))
        .whenComplete((balance, failure) -> {
          processingRefunds.remove(eventId);
          if (failure == null) {
            pendingRefunds.remove(eventId);
            Component message = refundMessages.remove(eventId);
            Bukkit.getScheduler().runTask(Community.get(), () -> {
              Player player = Bukkit.getPlayer(refund.playerId());
              if (player != null) {
                Audience viewer = Audience.get(player);
                if (refund.change().currency() == Currency.SPONSOR_TOKEN) {
                  viewer.sendMessage(formatTokenTransaction(
                      1,
                      message != null
                          ? message
                          : text(
                              "Your sponsor token has been refunded!",
                              NamedTextColor.GREEN,
                              TextDecoration.BOLD)));
                  viewer.playSound(Sounds.GET_TOKENS);
                } else {
                  viewer.sendMessage(text()
                      .append(MessageUtils.VOTE)
                      .appendSpace()
                      .append(
                          message != null
                              ? message
                              : text(
                                  "Your super vote has been refunded!",
                                  NamedTextColor.GREEN,
                                  TextDecoration.BOLD))
                      .build());
                }
              }
            });
          } else {
            // Message only, failed refunds retry every 30 seconds
            logger.warning(
                "Currency refund pending retry " + eventId + ": " + failure.getMessage());
          }
        });
  }

  public RequestConfig getRequestConfig() {
    return (RequestConfig) getConfig();
  }

  private CompletableFuture<RequestProfile> withCurrentBalance(
      UUID playerId, RequestProfile profile, Currency currency) {
    if (profile == null || !isAuditEnabled(currency)) {
      return CompletableFuture.completedFuture(profile);
    }
    int balance =
        currency == Currency.SPONSOR_TOKEN ? profile.getSponsorTokens() : profile.getSuperVotes();
    if (balance >= 1) return CompletableFuture.completedFuture(profile);
    return store.refresh(playerId);
  }

  private String getServerId() {
    return Community.get().getServerConfig().getServerId();
  }

  private void releaseSponsorClaim(UUID playerId) {
    if (!isAuditEnabled(Currency.SPONSOR_TOKEN)) return;
    store.releaseSponsorClaim(playerId, getServerId()).exceptionally(failure -> {
      logger.log(Level.WARNING, "Could not release sponsor slot for " + playerId, failure);
      return null;
    });
  }

  private static class SponsorClaimLostException extends RuntimeException {

    SponsorClaimLostException() {
      super("Sponsor slot was claimed by another server");
    }

    static boolean isCause(Throwable failure) {
      for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
        if (cause instanceof SponsorClaimLostException) return true;
      }
      return false;
    }
  }

  private boolean isAuditEnabled(Currency currency) {
    return currency == Currency.SPONSOR_TOKEN
        ? getRequestConfig().isSponsorAuditEnabled()
        : getRequestConfig().isSuperVoteAuditEnabled();
  }

  private boolean isAnyAuditEnabled() {
    return getRequestConfig().isSponsorAuditEnabled()
        || getRequestConfig().isSuperVoteAuditEnabled();
  }

  @Override
  public CompletableFuture<RequestProfile> onLogin(PlayerJoinEvent event) {
    return store.login(event.getPlayer().getUniqueId(), isAnyAuditEnabled());
  }

  @Override
  public void update(RequestProfile profile) {
    store
        .update(
            profile,
            !getRequestConfig().isSponsorAuditEnabled(),
            !getRequestConfig().isSuperVoteAuditEnabled())
        .thenRun(() -> network.sendUpdate(new RequestUpdate(profile.getPlayerId())));
  }

  @Override
  public void receiveSponsorClaim(UUID playerId) {
    if (!isAuditEnabled(Currency.SPONSOR_TOKEN)) return;
    Bukkit.getScheduler().runTask(Community.get(), () -> {
      if (sponsor.getSponsorQueue().removeIf(request -> request.playerId().equals(playerId))) {
        String name = users.getUsername(playerId);
        logAudit(
            Currency.SPONSOR_TOKEN,
            "&7Removed sponsor request for &f%s &8- &7replaced on another server",
            name != null ? name : playerId);
      }
    });
  }

  @Override
  public void receiveNetworkInvalidation(UUID playerId) {
    if (Bukkit.getPlayer(playerId) != null) {
      store.refresh(playerId);
    } else {
      store.invalidate(playerId);
    }
  }

  @Override
  public CompletableFuture<RequestProfile> getRequestProfile(UUID playerId) {
    return store.query(playerId.toString());
  }

  @Override
  public RequestProfile getCached(UUID playerId) {
    return store.getCached(playerId);
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

    MapInfo oldMap;
    if ((oldMap = requests.getIfPresent(player.getUniqueId())) != null) {

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

    getRequestProfile(player.getUniqueId())
        .thenCompose(
            profile -> withCurrentBalance(player.getUniqueId(), profile, Currency.SPONSOR_TOKEN))
        .thenAcceptAsync(profile -> {
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

          if (!isAuditEnabled(Currency.SPONSOR_TOKEN)) {
            queueSponsorRequest(player, viewer, map, null);
            return;
          }

          // Take the player's network-wide sponsor slot, replacing a request on another server
          store
              .claimSponsor(player.getUniqueId(), getServerId(), map.getId())
              .whenComplete((replaced, failure) -> {
                if (failure == null) {
                  // Remove any older request for this player still queued on another server
                  network.sendUpdate(new SponsorClaimUpdate(player.getUniqueId()));
                } else {
                  // Still queue it, a missing claim counts as owned when the request is processed
                  logger.log(
                      Level.WARNING,
                      "Could not claim sponsor slot for " + player.getName(),
                      failure);
                }
                queueSponsorRequest(
                    player, viewer, map, replaced != null ? replaced.orElse(null) : null);
              });
        });
  }

  private void queueSponsorRequest(
      Player player, Audience viewer, MapInfo map, String replacedServer) {
    Bukkit.getScheduler().runTask(Community.get(), () -> {
      if (sponsor.isQueued(player.getUniqueId())) {
        viewer.sendWarning(text("You already have a map in the sponsor queue."));
        return;
      }
      if (!player.isOnline()) {
        releaseSponsorClaim(player.getUniqueId());
        return;
      }
      if (sponsor.isMapQueued(map)) {
        releaseSponsorClaim(player.getUniqueId());
        viewer.sendWarning(text()
            .append(map.getStyledName(MapNameStyle.COLOR))
            .append(text(" is already in the queue!"))
            .build());
        return;
      }
      if (!sponsor.isQueueOpen()) {
        releaseSponsorClaim(player.getUniqueId());
        viewer.sendWarning(text("The sponsor queue is full! Please submit your request later."));
        return;
      }

      // Sponsor Queue
      // -> Add to queue, don't charge token until sponsor is processed
      sponsor.queueSponsorRequest(player, map);

      // Send confirmation, including map queue position
      viewer.sendMessage(SponsorComponents.getSuccessfulSponsorMessage(map));
      if (replacedServer != null) {
        viewer.sendMessage(text(
            "Your sponsor request on " + replacedServer + " has been replaced by this one",
            NamedTextColor.GRAY));
      }

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

    if (!hasSelectedMapForRunningVote(player)) {
      viewer.sendWarning(text("Select at least one map before activating a super vote!"));
      return;
    }

    if (!getRequestConfig().isSuperVoteAuditEnabled()) {
      getRequestProfile(player.getUniqueId()).thenAcceptAsync(profile -> {
        if (profile.getSuperVotes() < 1) {
          viewer.sendWarning(text("You don't have enough super votes!"));
          return;
        }
        profile.superVote();
        update(profile);
        superVotes.onActivate(player);
        superVotes.sendSuperVoterActivationFeedback(player);
      });
    } else {
      if (!processingSuperVotes.add(player.getUniqueId())) {
        viewer.sendWarning(text("Your super vote is already being processed!"));
        return;
      }
      UUID currentVote = voteId;
      CurrencyChange spend = new CurrencyChange(
          UUID.randomUUID(),
          Currency.SUPER_VOTE,
          -1,
          CurrencyAction.SPEND,
          player.getUniqueId(),
          player.getName(),
          "Vote " + currentVote + " on " + getCurrentMap().getId(),
          null);
      getRequestProfile(player.getUniqueId())
          .thenCompose(
              profile -> withCurrentBalance(player.getUniqueId(), profile, Currency.SUPER_VOTE))
          .thenCompose(profile -> {
            // Check the balance first so an empty balance never reaches the database
            if (profile != null && profile.getSuperVotes() < 1) {
              return CompletableFuture.<Integer>failedFuture(new InsufficientCurrencyException());
            }
            return changeCurrency(player.getUniqueId(), spend);
          })
          .whenComplete((balance, failure) -> Bukkit.getScheduler().runTask(Community.get(), () -> {
            processingSuperVotes.remove(player.getUniqueId());
            applyAuditedSuperVote(player, viewer, spend, currentVote, failure);

            // Re-Open the vote book once the super vote has been saved
            if (player.isOnline() && superVotes.isVotingActive()) {
              Bukkit.dispatchCommand(player, "votebook");
            }
          }));
      return;
    }

    // Re-Open the vote book
    Bukkit.dispatchCommand(player, "votebook");
  }

  private void applyAuditedSuperVote(
      Player player, Audience viewer, CurrencyChange spend, UUID currentVote, Throwable failure) {
    if (failure != null) {
      // The database may also reject a spend when the cached balance was stale
      if (InsufficientCurrencyException.isCause(failure)) {
        viewer.sendWarning(text("You don't have enough super votes!"));
      } else {
        viewer.sendWarning(text("Could not save your super vote. Please try again!"));
      }
      return;
    }
    if (!currentVote.equals(voteId)
        || !superVotes.isVotingActive()
        || !player.isOnline()
        || !hasSelectedMapForRunningVote(player)) {
      // The refund message tells the player what happened once the vote is back in their balance
      queueRefund(
          player.getUniqueId(),
          Currency.SUPER_VOTE,
          spend,
          "Vote ended before activation",
          text(
              "The vote ended before your super vote was activated, it has been refunded!",
              NamedTextColor.GREEN,
              TextDecoration.BOLD));
      return;
    }
    superVotes.onActivate(player);
    superVotes.sendSuperVoterActivationFeedback(player);
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    final Player player = event.getPlayer();

    superVotes.onRelogin(player);

    onLogin(event).thenAcceptAsync(profile -> {
      if (profile == null) {
        logger.warning("Could not load request profile for " + player.getName());
        // Only sponsors rely on the profile at login, for their token refresh
        if (player.hasPermission(CommunityPermissions.REQUEST_SPONSOR)) {
          Audience.get(player)
              .sendWarning(text("Could not load your sponsor tokens. Please rejoin to try again!"));
        }
        return;
      }
      if (player.hasPermission(CommunityPermissions.REQUEST_SPONSOR)) {
        sponsor.performTokenRefresh(player, profile);
      }
    });
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onMatchEnd(MatchFinishEvent event) {
    voteConfirm.invalidateAll();
    voteId = UUID.randomUUID();
    superVotes.onVoteStart();

    if (sponsor.getCurrentSponsor() != null) { // Reset current sponsor after match ends
      sponsorCharges.remove(sponsor.getCurrentSponsor().playerId());
      sponsor.setCurrentSponsor(null);
    }

    // Add cooldown for existing map and all variants
    sponsor.startNewMapCooldown(event.getMatch().getMap(), event.getMatch().getDuration());
    if (sponsor.getSponsorQueue().isEmpty()) return;

    MapPoolManager poolManager = getPoolManager();
    if (poolManager == null) return; // Cancel if pool manager not found

    // Remove any offline sponsor requests before selection
    removeOfflineSponsors();

    // Schedule the actual sponsoring for slightly after match end
    // This ensures if a restart, party start, or other sponsor-altering event is queued, it will
    // prevent
    PGM.get()
        .getExecutor()
        .schedule(
            () -> {
              VotePoolOptions options = poolManager.getVoteOptions();

              if (!options.getCustomVoteMaps().isEmpty()) return; // Already some vote-added map(s)
              if (poolManager.getOverriderMap() != null) return; // Set-nexted map
              if (isBlitz()) return; // Prevent sponsor after blitz map
              if (RestartManager.isQueued()) return; // No sponsor when restarting
              if (isPartyActive()) return; // No sponsor during parties

              SponsorRequest nextRequest = sponsor.getNextSponsor();

              if (nextRequest == null) return;
              if (processingSponsor) {
                sponsor.getSponsorQueue().addFirst(nextRequest);
                return;
              }
              if (!getRequestConfig().isSponsorAuditEnabled()) {
                // Notify PGM of the sponsored map and minimum cooldown
                options.addMap(nextRequest.map(), nextRequest.playerId());
                sponsor.startNewMapCooldown(nextRequest.map(), Duration.ZERO);

                // Track the current sponsor
                sponsor.setCurrentSponsor(nextRequest);

                // Update profile
                getRequestProfile(nextRequest.playerId()).thenAcceptAsync(profile -> {
                  // Update RequestProfile with sponsor map info
                  profile.sponsor(nextRequest.map());
                  update(profile);
                });

                // Alert online player if their sponsor request has been processed
                sponsor.alertRequesterToConfirmation(nextRequest.playerId());
                return;
              }
              processingSponsor = true;
              UUID currentVote = voteId;
              Player chargingPlayer = Bukkit.getPlayer(nextRequest.playerId());
              CurrencyChange charge = new CurrencyChange(
                  UUID.randomUUID(),
                  Currency.SPONSOR_TOKEN,
                  -1,
                  CurrencyAction.SPEND,
                  nextRequest.playerId(),
                  chargingPlayer == null
                      ? nextRequest.playerId().toString()
                      : chargingPlayer.getName(),
                  nextRequest.map().getId(),
                  null);
              store
                  .ownsSponsorClaim(nextRequest.playerId(), getServerId())
                  .exceptionally(failure -> {
                    logger.log(
                        Level.WARNING,
                        "Could not check sponsor slot for " + nextRequest.playerId(),
                        failure);
                    return true;
                  })
                  .thenCompose(owned -> owned
                      ? changeCurrency(nextRequest.playerId(), charge)
                      : CompletableFuture.<Integer>failedFuture(new SponsorClaimLostException()))
                  .whenComplete((balance, failure) -> Bukkit.getScheduler()
                      .runTask(Community.get(), () -> {
                        processingSponsor = false;
                        if (failure != null) {
                          Player player = Bukkit.getPlayer(nextRequest.playerId());
                          // Replaced by a newer request on another server, which owns it now
                          if (SponsorClaimLostException.isCause(failure)) {
                            logger.info(String.format(
                                "Removed sponsor request (%s) for %s - replaced on another server",
                                nextRequest.map().getName(), nextRequest.playerId()));
                            return;
                          }
                          // Retrying can't fix a low balance, so drop the request instead of
                          // letting it claim the sponsor slot every time it reaches the front
                          if (InsufficientCurrencyException.isCause(failure)) {
                            releaseSponsorClaim(nextRequest.playerId());
                            logger.info(String.format(
                                "Removed sponsor request (%s) for %s - not enough tokens",
                                nextRequest.map().getName(), nextRequest.playerId()));
                            if (player != null) {
                              Audience.get(player)
                                  .sendWarning(
                                      text(
                                          "You don't have enough sponsor tokens! Your sponsor request has been removed"));
                            }
                            return;
                          }
                          sponsor.getSponsorQueue().addFirst(nextRequest);
                          if (player != null) {
                            Audience.get(player)
                                .sendWarning(
                                    text(
                                        "Could not process your sponsor request. It will remain in the queue"));
                          }
                          return;
                        }
                        if (!currentVote.equals(voteId)
                            || !superVotes.isVotingActive()
                            || !options.getCustomVoteMaps().isEmpty()
                            || poolManager.getOverriderMap() != null
                            || RestartManager.isQueued()
                            || isPartyActive()) {
                          queueRefund(
                              nextRequest.playerId(),
                              Currency.SPONSOR_TOKEN,
                              charge,
                              "Vote changed before sponsor processing: "
                                  + nextRequest.map().getId(),
                              text(
                                  "The vote changed before your map was added, your request remains queued",
                                  NamedTextColor.GREEN,
                                  TextDecoration.BOLD));
                          sponsor.getSponsorQueue().addFirst(nextRequest);
                          return;
                        }
                        options.addMap(nextRequest.map(), nextRequest.playerId());
                        sponsor.startNewMapCooldown(nextRequest.map(), Duration.ZERO);
                        sponsor.setCurrentSponsor(nextRequest);
                        sponsorCharges.put(nextRequest.playerId(), charge);
                        releaseSponsorClaim(nextRequest.playerId());
                        sponsor.alertRequesterToConfirmation(nextRequest.playerId());
                      }));
            },
            1,
            TimeUnit.SECONDS);
  }

  @EventHandler
  public void onVoteEnd(MatchVoteFinishEvent event) {
    voteConfirm.invalidateAll();
    voteId = UUID.randomUUID();
    superVotes.onVoteEnd();

    SponsorRequest currentSponsor = sponsor.getCurrentSponsor();

    if (currentSponsor != null) {

      Player player = Bukkit.getPlayer(currentSponsor.playerId());

      // Same map = winner, refund the token even if offline
      if (currentSponsor.map().equals(event.getPickedMap()) && currentSponsor.canRefund()) {
        Component winMessage =
            text("Your sponsored map won the vote!", NamedTextColor.GREEN, TextDecoration.BOLD);
        if (getRequestConfig().isSponsorAuditEnabled()) {
          queueRefund(
              currentSponsor.playerId(),
              Currency.SPONSOR_TOKEN,
              sponsorCharges.get(currentSponsor.playerId()),
              currentSponsor.map().getId(),
              winMessage);
          return;
        }
        getRequestProfile(currentSponsor.playerId()).thenAcceptAsync(profile -> {
          profile.giveSponsorToken(1);
          update(profile);

          if (player != null) {
            Audience viewer = Audience.get(player);
            viewer.sendMessage(formatTokenTransaction(1, winMessage));
            viewer.playSound(Sounds.GET_TOKENS);
          }
        });
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerVote(MatchPlayerVoteEvent event) {
    superVotes.onVoteSelectionChange(event.getPlayer().getBukkit(), event.isAdd());
  }

  @EventHandler(priority = EventPriority.LOW)
  public void onMatchJoinMessage(PlayerJoinMatchEvent event) {
    MapInfo map = event.getMatch().getMap();

    if (getCurrentSponsor() == null) return;
    if (VisibilityUtils.isDisguised(getCurrentSponsor().playerId())) return;
    if (!getCurrentSponsor().map().equals(map)) return;

    event.getExtraLines().add(empty());
    event
        .getExtraLines()
        .add(SponsorComponents.getSponsoredJoinMessage(getCurrentSponsor().playerId()));
  }

  private final Cache<UUID, String> voteConfirm =
      CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.SECONDS).build();

  private static final List<String> BLOCKED_COMMANDS =
      List.of("/vote add", "/pgm:vote add", "/sn", "/setnext", "/pgm:sn", "/pgm:setnext");

  public static boolean isBlockedCommand(String command) {
    String normalized = command.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    return BLOCKED_COMMANDS.stream()
        .anyMatch(blocked -> normalized.equals(blocked) || normalized.startsWith(blocked + " "));
  }

  private boolean hasSponsoredVote() {
    SponsorRequest current = sponsor.getCurrentSponsor();
    if (current == null || !superVotes.isVotingActive()) return false;

    MapPoolManager poolManager = getPoolManager();
    if (poolManager == null || poolManager.getOverriderMap() != null) return false;

    if (poolManager.getActiveMapPool() instanceof VotingPool pool) {
      MapPoll poll = pool.getCurrentPoll();
      if (poll != null && poll.isRunning()) {
        return poll.getVotes().containsKey(current.map());
      }
    }

    // The sponsor is added shortly after match end, before the poll may have started.
    return poolManager.getVoteOptions().isMapAdded(current.map());
  }

  @EventHandler(ignoreCancelled = true)
  public void onVoteAddCommand(PlayerCommandPreprocessEvent event) {
    if (!isBlockedCommand(event.getMessage())) return;
    UUID playerId = event.getPlayer().getUniqueId();
    if (!hasSponsoredVote()) {
      voteConfirm.invalidate(playerId);
      return;
    }
    if (event.getMessage().equals(voteConfirm.getIfPresent(playerId))) {
      voteConfirm.invalidate(playerId);
      return;
    }
    event.setCancelled(true);
    voteConfirm.put(playerId, event.getMessage());
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
    boolean removed = this.sponsor.getSponsorQueue().removeIf(s -> s.playerId().equals(playerId));
    if (removed) releaseSponsorClaim(playerId);
    return removed;
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
        .filter(sr -> sr.playerId().equals(playerId))
        .findAny();
  }

  @Override
  public int queueIndex(SponsorRequest request) {
    return this.sponsor.getSponsorQueue().indexOf(request);
  }

  private void removeOfflineSponsors() {
    Iterator<SponsorRequest> iterator = sponsor.getSponsorQueue().iterator();
    while (iterator.hasNext()) {
      SponsorRequest request = iterator.next();
      Player player = Bukkit.getPlayer(request.playerId());

      if (player == null || !player.isOnline()) {
        iterator.remove();
        releaseSponsorClaim(request.playerId());

        logger.info("Removed offline sponsor request:" + request.playerId() + " for map "
            + request.map().getName());
      }
    }
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
  public boolean hasSelectedMapForRunningVote(Player player) {
    return superVotes.isVotingActive() && superVotes.hasSelectedMap(player);
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
          .clickEvent(ClickEvent.runCommand("/sponsor request " + map.getName()))
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

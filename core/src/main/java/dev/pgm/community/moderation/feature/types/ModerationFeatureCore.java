package dev.pgm.community.moderation.feature.types;

import static net.kyori.adventure.text.Component.text;
import static tc.oc.pgm.util.player.PlayerComponent.player;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import dev.pgm.community.Community;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.events.PlayerPardonEvent;
import dev.pgm.community.events.PlayerPunishmentEvent;
import dev.pgm.community.feature.FeatureBase;
import dev.pgm.community.moderation.ModerationConfig;
import dev.pgm.community.moderation.feature.ModerationFeature;
import dev.pgm.community.moderation.feature.PGMPunishmentIntegration;
import dev.pgm.community.moderation.feature.loggers.BlockGlitchLogger;
import dev.pgm.community.moderation.feature.loggers.SignLogger;
import dev.pgm.community.moderation.punishments.NetworkPunishment;
import dev.pgm.community.moderation.punishments.Punishment;
import dev.pgm.community.moderation.punishments.PunishmentFormats;
import dev.pgm.community.moderation.punishments.PunishmentType;
import dev.pgm.community.moderation.punishments.types.MutePunishment;
import dev.pgm.community.moderation.store.ModerationStore;
import dev.pgm.community.moderation.tools.ModerationTools;
import dev.pgm.community.network.feature.NetworkFeature;
import dev.pgm.community.network.subs.types.PunishmentSubscriber;
import dev.pgm.community.network.updates.types.PunishmentUpdate;
import dev.pgm.community.network.updates.types.RefreshPunishmentUpdate;
import dev.pgm.community.users.feature.UsersFeature;
import dev.pgm.community.utils.BroadcastUtils;
import dev.pgm.community.utils.CommandAudience;
import dev.pgm.community.utils.NameUtils;
import dev.pgm.community.utils.PGMUtils;
import dev.pgm.community.utils.Sounds;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.named.NameStyle;

public class ModerationFeatureCore extends FeatureBase implements ModerationFeature {

  private final ModerationStore store;
  private final UsersFeature users;
  private final NetworkFeature network;
  private final Set<Punishment> recents;
  private final Cache<UUID, MutePunishment> muteCache;
  private final Cache<UUID, Set<String>> banEvasionCache;
  private final Cache<UUID, Punishment> observerBanCache;
  private final Cache<UUID, Instant> pardonedPlayers;
  private final Cache<UUID, Punishment> matchBan;

  private PGMPunishmentIntegration integration;
  private SignLogger signLogger;
  private BlockGlitchLogger blockGlitchLogger;

  private boolean color = false;

  public ModerationFeatureCore(
      Configuration config,
      Logger logger,
      UsersFeature users,
      NetworkFeature network,
      ModerationStore store) {
    super(new ModerationConfig(config), logger, "Punishments");
    this.store = store;
    this.users = users;
    this.network = network;
    this.recents = Sets.newHashSet();
    this.muteCache = CacheBuilder.newBuilder().build();
    this.banEvasionCache = CacheBuilder.newBuilder()
        .expireAfterWrite(getModerationConfig().getEvasionExpireMins(), TimeUnit.MINUTES)
        .build();
    this.observerBanCache = CacheBuilder.newBuilder().build();
    this.pardonedPlayers = CacheBuilder.newBuilder().build();
    this.matchBan = getModerationConfig().getMatchBanDuration() == null
        ? null
        : CacheBuilder.newBuilder()
            .expireAfterWrite(
                getModerationConfig().getMatchBanDuration().getSeconds(), TimeUnit.SECONDS)
            .build();

    if (getConfig().isEnabled()) {
      enable();

      if (getModerationConfig().isSignLoggerEnabled()) this.signLogger = new SignLogger();
      // Set PGM punishment integration
      if (PGMUtils.isPGMEnabled()) {
        this.integration = new PGMPunishmentIntegration(this);
        this.integration.enable();

        // BG uses pgm dependencies, only enable if pgm is loaded
        if (getModerationConfig().isBlockGlitchLoggerEnabled())
          this.blockGlitchLogger = new BlockGlitchLogger();
      }

      Community.get()
          .getServer()
          .getScheduler()
          .scheduleSyncRepeatingTask(Community.get(), this::banHover, 0, 20L);

      // Register punishment subscriber
      network.registerSubscriber(new PunishmentSubscriber(this, network.getNetworkId(), logger));
    }
  }

  public NetworkFeature getNetwork() {
    return network;
  }

  public UsersFeature getUsers() {
    return users;
  }

  public ModerationConfig getModerationConfig() {
    return (ModerationConfig) getConfig();
  }

  @Override
  public void save(Punishment punishment) {
    if (getModerationConfig().isPersistent()) {

      // When issuing a new ban or mute, check for existing and pardon if any.
      switch (punishment.getType()) {
        case TEMP_BAN:
        case BAN:
          isBanned(punishment.getTargetId().toString()).thenAcceptAsync(banned -> {
            if (banned) {
              store
                  .pardon(punishment.getTargetId(), punishment.getIssuerId())
                  .thenAcceptAsync(x -> store.save(punishment));
            } else {
              store.save(punishment);
            }
          });
          break;
        case MUTE:
          isMuted(punishment.getTargetId()).thenAcceptAsync(mute -> {
            if (mute.isPresent()) {
              store
                  .unmute(punishment.getTargetId(), punishment.getIssuerId())
                  .thenAcceptAsync(x -> store.save(punishment));
            } else {
              store.save(punishment);
            }
          });
          break;
        default:
          store.save(punishment);
          break;
      }
    }
  }

  @Override
  public CompletableFuture<List<Punishment>> query(String target) {
    if (NameUtils.isMinecraftName(target)) {
      // CONVERT TO UUID if username
      return getUsers()
          .getStoredId(target)
          .thenApplyAsync(uuid -> uuid != null && uuid.isPresent()
              ? store.queryList(uuid.get().toString()).join()
              : Lists.newArrayList());
    }
    return store.queryList(target);
  }

  @Override
  public CompletableFuture<Boolean> pardon(String target, @Nullable CommandAudience issuer) {
    CompletableFuture<Optional<UUID>> playerId = NameUtils.isMinecraftName(target)
        ? getUsers().getStoredId(target)
        : CompletableFuture.completedFuture(Optional.of(UUID.fromString(target)));
    return playerId.thenComposeAsync(uuid -> {
      // Query active punishments first to know which types exist
      return uuid.map(value -> store.queryList(value.toString()).thenComposeAsync(punishments -> {
            List<PunishmentType> activeBanTypes = punishments.stream()
                .filter(p -> p.isActive() && p.getType().isLoginPrevented())
                .map(Punishment::getType)
                .toList();

            if (!activeBanTypes.isEmpty()) {
              UUID issuerId = issuer != null ? issuer.getId().orElse(null) : null;
              return store.pardon(value, issuerId).thenApplyAsync(success -> {
                if (success) {
                  for (PunishmentType type : activeBanTypes) {
                    Community.get().callEvent(new PlayerPardonEvent(issuer, value, type));
                  }
                  sendRefresh(value);
                  removeCachedBan(value);
                }
                return success;
              });
            }
            return CompletableFuture.completedFuture(false);
          }))
          .orElseGet(() -> CompletableFuture.completedFuture(false));
    });
  }

  @Override
  public CompletableFuture<Boolean> deactivate(String target, PunishmentType punishmentType) {
    CompletableFuture<Optional<UUID>> playerId = NameUtils.isMinecraftName(target)
        ? getUsers().getStoredId(target)
        : CompletableFuture.completedFuture(Optional.of(UUID.fromString(target)));
    return playerId.thenApplyAsync(uuid -> {
      if (uuid.isPresent()) {
        if (store.deactivate(uuid.get(), punishmentType).join()) {
          sendRefresh(uuid.get());
          return true;
        }
      }
      return false;
    });
  }

  @Override
  public CompletableFuture<Boolean> isBanned(String target) {
    if (NameUtils.isMinecraftName(target)) {
      return getUsers()
          .getStoredId(target)
          .thenApplyAsync(
              uuid -> uuid.isPresent() ? store.isBanned(uuid.get().toString()).join() : false);
    }
    return store.isBanned(target);
  }

  @Override
  public CompletableFuture<Optional<Punishment>> getActiveBan(String target) {
    if (NameUtils.isMinecraftName(target)) {
      return getUsers()
          .getStoredId(target)
          .thenApplyAsync(uuid -> uuid.isPresent()
              ? store.getActiveBan(uuid.get().toString()).join()
              : Optional.empty());
    }
    return store.getActiveBan(target);
  }

  @Override
  public void onPreLogin(AsyncPlayerPreLoginEvent event) {
    List<Punishment> punishments;
    try {
      punishments = store
          .queryActiveForLogin(event.getUniqueId().toString())
          .get(getModerationConfig().getLoginTimeout(), TimeUnit.SECONDS);

      Optional<Punishment> ban = hasActiveBan(punishments);
      if (ban.isPresent()) {
        Punishment punishment = ban.get();
        event.setKickMessage(punishment.formatPunishmentScreen(
            getModerationConfig(),
            getUsers().renderUsername(punishment.getIssuerId(), NameStyle.FANCY).join(),
            false));
        event.setLoginResult(Result.KICK_BANNED);

        if (punishment.getType() == PunishmentType.NAME_BAN) {
          String bannedName = punishment.getReason();
          if (!event.getName().equalsIgnoreCase(bannedName)) {
            pardon(punishment.getTargetId().toString(), null);
            event.setLoginResult(Result.ALLOWED);
            logger.info(String.format(
                "Name change detected for (%s) | %s -> %s | Account unbanned",
                punishment.getTargetId().toString(), punishment.getReason(), event.getName()));
          }
        }
      }

      Optional<MutePunishment> mute = hasActiveMute(punishments);
      mute.ifPresent(mutePunishment -> addMute(event.getUniqueId(), mutePunishment));

      Set<Punishment> deferredPunishments = getDeferredPunishments(punishments);
      for (Punishment punishment : deferredPunishments) {
        Bukkit.getScheduler()
            .runTaskLater(
                Community.get(),
                () -> {
                  if (PunishmentType.WARN.equals(punishment.getType())
                      || PunishmentType.KICK.equals(punishment.getType())) {
                    if (punishment.punish(true)) {
                      deactivate(event.getUniqueId().toString(), punishment.getType());
                    }
                  }
                },
                20 * 5);
      }

      logger.info(punishments.size()
          + " Punishments have been fetched for "
          + event.getUniqueId().toString());
    } catch (InterruptedException | ExecutionException e) {
      event.setLoginResult(Result.KICK_OTHER);
      event.setKickMessage(
          ChatColor.DARK_RED + "Error joining, please try again."); // TODO: Pretty this up
      e.printStackTrace();
    } catch (TimeoutException e) {
      scheduleDelayedCheck(event.getUniqueId());
    }
  }

  private void scheduleDelayedCheck(UUID playerId) {
    Community.get().getServer().getScheduler().scheduleSyncDelayedTask(Community.get(), () -> {
      Player player = Bukkit.getPlayer(playerId);
      if (player != null) {
        store.queryActiveForLogin(playerId.toString()).thenAcceptAsync(punishments -> {
          Optional<Punishment> ban = hasActiveBan(punishments);
          ban.ifPresent(punishment -> player.kickPlayer(punishment.formatPunishmentScreen(
              getModerationConfig(),
              getUsers()
                  .renderUsername(punishment.getIssuerId(), NameStyle.FANCY)
                  .join(),
              false)));

          Optional<MutePunishment> mute = hasActiveMute(punishments);
          mute.ifPresent(mutePunishment -> addMute(playerId, mutePunishment));

          logger.info("[Delayed]: "
              + punishments.size()
              + " Punishments have been fetched for "
              + playerId);
        });
      }
    });
  }

  private Optional<MutePunishment> hasActiveMute(List<Punishment> punishments) {
    return punishments.stream()
        .filter(p -> p.isActive()
            && p.getType().equals(PunishmentType.MUTE)
            && p.getService().equalsIgnoreCase(getModerationConfig().getService()))
        .map(MutePunishment.class::cast)
        .findAny();
  }

  private Optional<Punishment> hasActiveBan(List<Punishment> punishments) {
    return punishments.stream()
        .filter(p -> p.isActive()
            && p.getType().isLoginPrevented()
            && p.getService().equalsIgnoreCase(getModerationConfig().getService()))
        .findAny();
  }

  private Set<Punishment> getDeferredPunishments(List<Punishment> punishments) {
    return punishments.stream()
        .filter(p -> p.isActive()
            && (PunishmentType.WARN.equals(p.getType()) || PunishmentType.KICK.equals(p.getType())))
        .collect(Collectors.toSet());
  }

  @Override
  public CompletableFuture<Optional<Punishment>> isMuted(UUID target) {
    return store.isMuted(target);
  }

  @Override
  public CompletableFuture<Boolean> unmute(UUID id, @Nullable CommandAudience issuer) {
    UUID issuerId = issuer != null ? issuer.getId().orElse(null) : null;
    return store.unmute(id, issuerId).thenApplyAsync(success -> {
      if (success) {
        // Fire pardon event before refresh
        Community.get().callEvent(new PlayerPardonEvent(issuer, id, PunishmentType.MUTE));
        removeMute(id);
        sendRefresh(id); // Successful unmute will update other servers
      }
      return success;
    });
  }

  @Override
  public CompletableFuture<List<Punishment>> getRecentPunishments(Duration period) {
    return store.getRecentPunishments(period);
  }

  @Override
  public CompletableFuture<Integer> count() {
    return store.count();
  }

  @Override
  public void recieveRefresh(UUID playerId) {
    store.invalidate(playerId);
    removeCachedBan(playerId);
    removeMute(playerId);
  }

  @Override
  public Punishment punish(
      PunishmentType type,
      UUID target,
      CommandAudience issuer,
      String reason,
      Duration duration,
      boolean active,
      boolean silent) {
    Instant time = Instant.now();
    Punishment punishment = Punishment.of(
        UUID.randomUUID(),
        target,
        getSenderId(issuer.getSender()),
        reason,
        time.toEpochMilli(),
        duration,
        type,
        active,
        time.toEpochMilli(),
        getSenderId(issuer.getSender()),
        getModerationConfig().getService());
    Bukkit.getPluginManager().callEvent(new PlayerPunishmentEvent(issuer, punishment, silent));
    return punishment;
  }

  @Override
  public ModerationTools getTools() {
    return integration != null ? integration.getTools() : null;
  }

  @Override
  public BlockGlitchLogger getBlockGlitchLogger() {
    return blockGlitchLogger;
  }

  @Override
  public Optional<Punishment> getLastPunishment(UUID issuer) {
    return recents.stream()
        .filter(p -> !p.isConsole() && p.getIssuerId().equals(issuer))
        .sorted()
        .findFirst();
  }

  @Override
  public Set<Player> getOnlineMutes() {
    return Bukkit.getOnlinePlayers().stream()
        .filter(pl -> getCachedMute(pl.getUniqueId()).isPresent())
        .collect(Collectors.toSet());
  }

  // Networking
  @Override
  public void sendUpdate(NetworkPunishment punishment) {
    network.sendUpdate(new PunishmentUpdate(punishment)); // Send out punishment update
  }

  @Override
  public void recieveUpdate(NetworkPunishment punishment) {
    recieveRefresh(punishment.getPunishment().getTargetId());
    broadcastPunishment(punishment.getPunishment(), true, punishment.getServer());
    // Extra step due to gson limitation (maybe look into type tokens)
    Punishment typedPunishment = Punishment.of(punishment.getPunishment());
    Community.get()
        .getServer()
        .getScheduler()
        .scheduleSyncDelayedTask(Community.get(), () -> typedPunishment.punish(true));
  }

  @Override
  public void sendRefresh(UUID playerId) {
    network.sendUpdate(new RefreshPunishmentUpdate(playerId));
  }

  @Override
  public String getGlobalFormat() {
    return getModerationConfig().getGlobalBroadcastFormat();
  }

  @Override
  public String getStaffFormat() {
    return getModerationConfig().getStaffBroadcastFormat();
  }

  // EVENTS
  @EventHandler(priority = EventPriority.LOWEST)
  public void onPunishmentEvent(PlayerPunishmentEvent event) {
    final Punishment punishment = event.getPunishment();

    Optional<Player> onlineTarget = punishment.getTargetPlayer();
    if (onlineTarget.isPresent()) {
      if (!event.getSender().hasPermission(CommunityPermissions.ADMIN)
          && onlineTarget.get().hasPermission(CommunityPermissions.ADMIN)) {
        event
            .getSender()
            .sendWarning(text()
                .append(player(onlineTarget.get(), NameStyle.FANCY))
                .append(text(" is exempt from punishment"))
                .build());
        return;
      }
    } else if (PunishmentType.WARN.equals(punishment.getType())
        || PunishmentType.KICK.equals(punishment.getType())) {
      // If it's a warn or kick set it to active so the player sees it when they next login
      punishment.setActive(true);
    }

    save(punishment); // Save punishment to database

    recents.add(punishment); // Cache recent punishment

    punishment.punish(event.isSilent()); // Perform the actual punishment

    sendUpdate(new NetworkPunishment(
        punishment, network.getNetworkId())); // Send out network punishment update

    switch (punishment.getType()) {
      // Cache known IPS of a recently banned player, so if they rejoin on an alt we can find them
      case BAN:
      case TEMP_BAN:
      case NAME_BAN:
        users
            .getKnownIPs(punishment.getTargetId())
            .thenAcceptAsync(ips -> banEvasionCache.put(punishment.getTargetId(), ips));
        break;
      case MUTE: // Cache mute for easy lookup for sign/chat events
        addMute(punishment.getTargetId(), (MutePunishment) punishment);
        break;
      case KICK:
        if (matchBan != null) { // Store match ban
          matchBan.put(event.getPunishment().getTargetId(), punishment);
        }
        break;
      default:
        break;
    }

    broadcastPunishment(punishment, event.isSilent());
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onPreLoginEvent(AsyncPlayerPreLoginEvent event) {
    this.onPreLogin(event);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerJoinEvasionCheck(PlayerJoinEvent event) {
    String host = event.getPlayer().getAddress().getAddress().getHostAddress();
    Optional<UUID> banEvasion = isBanEvasion(host);
    boolean exclude = hasRecentPardon(event.getPlayer().getUniqueId());

    if (banEvasion.isPresent() && !exclude) {
      users.renderUsername(banEvasion, NameStyle.FANCY).thenAcceptAsync(bannedName -> {
        if (!banEvasion.get().equals(event.getPlayer().getUniqueId())) {
          BroadcastUtils.sendAdminChatMessage(
              PunishmentFormats.formatBanEvasion(event.getPlayer(), banEvasion.get(), bannedName),
              Sounds.BAN_EVASION,
              CommunityPermissions.UNBAN);
        }
      });
    }
  }

  // Cancel chat for muted/banned players
  @EventHandler(priority = EventPriority.HIGHEST)
  public void onAsyncPlayerChatEvent(AsyncPlayerChatEvent event) {
    // MUTES
    getCachedMute(event.getPlayer().getUniqueId()).ifPresent(mute -> {
      event.setCancelled(true);
      Audience.get(event.getPlayer()).sendWarning(mute.getChatMuteMessage());
    });
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onPlaceSign(SignChangeEvent event) {
    // Prevent muted players from using signs
    Optional<MutePunishment> mute = getCachedMute(event.getPlayer().getUniqueId());
    if (mute.isEmpty()) return;

    if (Arrays.stream(event.getLines()).allMatch(String::isBlank)) return;
    for (int i = 0; i < 4; i++) event.setLine(i, " ");
    Audience.get(event.getPlayer()).sendWarning(mute.get().getSignMuteMessage());
  }

  // BANS
  @Nullable
  public Cache<UUID, Punishment> getMatchBans() {
    return matchBan;
  }

  protected void removeCachedBan(UUID playerId) {
    banEvasionCache.invalidate(playerId);
    pardonedPlayers.put(playerId, Instant.now());
  }

  // MUTES
  protected void addMute(UUID playerId, MutePunishment punishment) {
    muteCache.put(playerId, punishment);
  }

  protected void removeMute(UUID playerId) {
    muteCache.invalidate(playerId);
  }

  @Override
  public Optional<MutePunishment> getCachedMute(UUID playerId) {
    MutePunishment mute = muteCache.getIfPresent(playerId);
    if (mute != null && !mute.isActive()) {
      muteCache.invalidate(playerId);
      return Optional.empty();
    }
    return Optional.ofNullable(mute);
  }

  // ETC.
  @Nullable
  private UUID getSenderId(CommandSender sender) {
    return sender instanceof Player p ? p.getUniqueId() : null;
  }

  private Optional<UUID> isBanEvasion(String address) {
    return banEvasionCache.asMap().entrySet().stream()
        .filter(s -> s.getValue().contains(address))
        .map(Entry::getKey)
        .findAny();
  }

  private boolean hasRecentPardon(UUID playerId) {
    return pardonedPlayers.getIfPresent(playerId) != null;
  }

  private void banHover() {
    if (observerBanCache.asMap().isEmpty()) return;

    color = !color;
    NamedTextColor alertColor = color ? NamedTextColor.YELLOW : NamedTextColor.DARK_RED;
    Component warning = text(" \u26a0 ", alertColor);
    Component banned = text()
        .append(warning)
        .append(text("You have been banned", NamedTextColor.RED, TextDecoration.BOLD))
        .append(warning)
        .build();

    this.observerBanCache.asMap().keySet().stream()
        .map(Bukkit::getPlayer)
        .filter(Objects::nonNull)
        .map(Audience::get)
        .forEach(viewer -> viewer.sendActionBar(banned));
  }

  private Audience getStaffAudience() {
    List<Player> staff = Bukkit.getOnlinePlayers().stream()
        .filter(p -> p.hasPermission(CommunityPermissions.PUNISHMENT_BROADCASTS))
        .collect(Collectors.toList());
    return Audience.get(staff);
  }

  private Audience getGlobalAudience() {
    List<Player> normal = Bukkit.getOnlinePlayers().stream()
        .filter(p -> !p.hasPermission(CommunityPermissions.PUNISHMENT_BROADCASTS))
        .collect(Collectors.toList());
    return Audience.get(normal);
  }

  private void broadcastPunishment(Punishment punishment, boolean silent) {
    broadcastPunishment(punishment, silent, null);
  }

  private void broadcastPunishment(Punishment punishment, boolean silent, @Nullable String server) {
    boolean global = !silent && getModerationConfig().isPunishmentPublic(punishment);

    if (global) {
      PunishmentFormats.formatBroadcast(punishment, server, getGlobalFormat(), users)
          .thenAcceptAsync(broadcast -> getGlobalAudience().sendMessage(broadcast));
    }

    PunishmentFormats.formatBroadcast(punishment, server, getStaffFormat(), users)
        .thenAcceptAsync(broadcast -> BroadcastUtils.sendAdminChatMessage(
            broadcast, CommunityPermissions.PUNISHMENT_BROADCASTS));
  }
}

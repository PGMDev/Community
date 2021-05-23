package dev.pgm.community.moderation.feature;

import static net.kyori.adventure.text.Component.text;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Sets;
import dev.pgm.community.Community;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.events.PlayerPunishmentEvent;
import dev.pgm.community.feature.FeatureBase;
import dev.pgm.community.moderation.ModerationConfig;
import dev.pgm.community.moderation.commands.BanCommand;
import dev.pgm.community.moderation.commands.KickCommand;
import dev.pgm.community.moderation.commands.MuteCommand;
import dev.pgm.community.moderation.commands.PunishmentCommand;
import dev.pgm.community.moderation.commands.WarnCommand;
import dev.pgm.community.moderation.punishments.NetworkPunishment;
import dev.pgm.community.moderation.punishments.Punishment;
import dev.pgm.community.moderation.punishments.PunishmentFormats;
import dev.pgm.community.moderation.punishments.PunishmentType;
import dev.pgm.community.moderation.punishments.types.MutePunishment;
import dev.pgm.community.network.feature.NetworkFeature;
import dev.pgm.community.network.subs.types.PunishmentSubscriber;
import dev.pgm.community.network.updates.types.PunishmentUpdate;
import dev.pgm.community.network.updates.types.RefreshPunishmentUpdate;
import dev.pgm.community.users.feature.UsersFeature;
import dev.pgm.community.utils.BroadcastUtils;
import dev.pgm.community.utils.CommandAudience;
import dev.pgm.community.utils.PGMUtils;
import dev.pgm.community.utils.Sounds;
import java.time.Duration;
import java.time.Instant;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.named.NameStyle;

public abstract class ModerationFeatureBase extends FeatureBase implements ModerationFeature {

  private final UsersFeature users;
  private final NetworkFeature network;
  private final Set<Punishment> recents;
  private final Cache<UUID, MutePunishment> muteCache;
  private final Cache<UUID, Set<String>> banEvasionCache;
  private final Cache<UUID, Punishment> observerBanCache;
  private final Cache<UUID, Instant> pardonedPlayers;
  private Cache<UUID, Punishment> matchBan;

  private PGMPunishmentIntegration integration;
  private boolean color = false;

  public ModerationFeatureBase(
      ModerationConfig config,
      Logger logger,
      String featureName,
      UsersFeature users,
      NetworkFeature network) {
    super(config, logger, featureName);
    this.users = users;
    this.network = network;
    this.recents = Sets.newHashSet();
    this.muteCache = CacheBuilder.newBuilder().build();
    this.banEvasionCache =
        CacheBuilder.newBuilder()
            .expireAfterWrite(config.getEvasionExpireMins(), TimeUnit.MINUTES)
            .build();
    this.observerBanCache = CacheBuilder.newBuilder().build();
    this.pardonedPlayers = CacheBuilder.newBuilder().build();

    if (config.getMatchBanDuration() != null) {
      this.matchBan =
          CacheBuilder.newBuilder()
              .expireAfterWrite(config.getMatchBanDuration().getSeconds(), TimeUnit.SECONDS)
              .build();
    }

    if (config.isEnabled()) {
      enable();

      // Set PGM punishment integration
      if (PGMUtils.isPGMEnabled()) {
        this.integration = new PGMPunishmentIntegration(this);
        this.integration.enable();
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
  public Punishment punish(
      PunishmentType type,
      UUID target,
      CommandAudience issuer,
      String reason,
      Duration duration,
      boolean active,
      boolean silent) {
    Instant time = Instant.now();
    Punishment punishment =
        Punishment.of(
            UUID.randomUUID(),
            target,
            getSenderId(issuer.getSender()),
            reason,
            time,
            duration,
            type,
            active,
            time,
            getSenderId(issuer.getSender()),
            getModerationConfig().getService());
    save(punishment);
    Bukkit.getPluginManager().callEvent(new PlayerPunishmentEvent(issuer, punishment, silent));
    return punishment;
  }

  @Override
  public Set<CommunityCommand> getCommands() {
    Set<CommunityCommand> commands = Sets.newHashSet();

    if (getModerationConfig().isWarnEnabled()) {
      commands.add(new WarnCommand());
    }

    if (getModerationConfig().isKickEnabled()) {
      commands.add(new KickCommand());
    }

    if (getModerationConfig().isBanEnabled()) {
      commands.add(new BanCommand());
    }

    if (getModerationConfig().isMuteEnabled()) {
      commands.add(new MuteCommand());
    }

    commands.add(new PunishmentCommand());

    return commands;
  }

  @Override
  public Optional<Punishment> getLastPunishment(UUID issuer) {
    return recents.stream()
        .filter(p -> p.getIssuerId().isPresent() && p.getIssuerId().get().equals(issuer))
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
    broadcastPunishment(punishment.getPunishment(), true, punishment.getServer(), null);
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

  // EVENTS
  @EventHandler(priority = EventPriority.LOWEST)
  public void onPunishmentEvent(PlayerPunishmentEvent event) {
    final Punishment punishment = event.getPunishment();

    recents.add(punishment); // Cache recent punishment

    punishment.punish(event.isSilent()); // Perform the actual punishment

    sendUpdate(
        new NetworkPunishment(
            punishment, network.getNetworkId())); // Send out network punishment update

    switch (punishment.getType()) {
      case BAN:
      case TEMP_BAN: // Cache known IPS of a recently banned player, so if they rejoin on an alt can
        // find them
        addBan(punishment.getId(), punishment);
        users
            .getKnownIPs(punishment.getTargetId())
            .thenAcceptAsync(ips -> banEvasionCache.put(punishment.getTargetId(), ips));
        break;
      case MUTE: // Cache mute for easy lookup for sign/chat events
        addMute(punishment.getTargetId(), MutePunishment.class.cast(punishment));
        break;
      case KICK:
        if (matchBan != null) { // Store match ban
          matchBan.put(event.getPunishment().getTargetId(), punishment);
        }
        break;
      default:
        break;
    }

    broadcastPunishment(punishment, event.isSilent(), event.getSender().getAudience());
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onPreLoginEvent(AsyncPlayerPreLoginEvent event) {
    this.onPreLogin(event);
  }

  @EventHandler(priority = EventPriority.LOW)
  public void onPlayerJoinBanned(PlayerJoinEvent event) {
    getOnlineBan(event.getPlayer().getUniqueId())
        .ifPresent(
            ban -> {
              event.setJoinMessage(null); // Hide join message
              integration.updateBanPrefix(event.getPlayer(), true); // assign banned group
              Bukkit.getScheduler()
                  .runTaskLater(
                      Community.get(),
                      () ->
                          PunishmentFormats.sendNoPlayBanWelcome(
                              event.getPlayer(), ban, getModerationConfig().getAppealMessage()),
                      15L);
            });
  }

  @EventHandler(priority = EventPriority.LOW)
  public void onPlayerQuitBanned(PlayerQuitEvent event) {
    getOnlineBan(event.getPlayer().getUniqueId())
        .ifPresent(
            ban -> {
              event.setQuitMessage(null); // Hide quit message
              observerBanCache.invalidate(event.getPlayer().getUniqueId());
            });
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerJoinEvasionCheck(PlayerJoinEvent event) {
    String host = event.getPlayer().getAddress().getAddress().getHostAddress();
    Optional<UUID> banEvasion = isBanEvasion(host);
    boolean exclude = hasRecentPardon(event.getPlayer().getUniqueId());

    if (banEvasion.isPresent() && !exclude) {
      users
          .renderUsername(banEvasion, NameStyle.FANCY, null)
          .thenAcceptAsync(
              bannedName -> {
                if (!banEvasion.get().equals(event.getPlayer().getUniqueId())) {
                  BroadcastUtils.sendAdminChatMessage(
                      PunishmentFormats.formatBanEvasion(
                          event.getPlayer(), banEvasion.get(), bannedName),
                      Sounds.BAN_EVASION);
                }
              });
    }
  }

  // Cancel ALL commands for shadow banned
  @EventHandler(priority = EventPriority.MONITOR)
  public void onCommand(PlayerCommandPreprocessEvent event) {
    getOnlineBan(event.getPlayer().getUniqueId())
        .ifPresent(
            ban -> {
              event.setCancelled(true);
              Audience.get(event.getPlayer())
                  .sendWarning(PunishmentFormats.formatBanDenyError(ban, "use commands"));
            });
  }

  // Cancel chat for muted/banned players
  @EventHandler(priority = EventPriority.HIGHEST)
  public void onAsyncPlayerChatEvent(AsyncPlayerChatEvent event) {
    // Observer Banned
    getOnlineBan(event.getPlayer().getUniqueId())
        .ifPresent(
            ban -> {
              event.setCancelled(true);
              Audience.get(event.getPlayer())
                  .sendWarning(PunishmentFormats.formatBanDenyError(ban, "chat"));
            });

    // MUTES
    getCachedMute(event.getPlayer().getUniqueId())
        .ifPresent(
            mute -> {
              event.setCancelled(true);
              Audience.get(event.getPlayer()).sendWarning(mute.getChatMuteMessage());
            });
  }

  // Clear sign text for muted players
  @EventHandler(priority = EventPriority.HIGHEST)
  public void onPlaceSign(SignChangeEvent event) {
    getCachedMute(event.getPlayer().getUniqueId())
        .ifPresent(
            mute -> {
              for (int i = 0; i < 4; i++) {
                event.setLine(i, " ");
              }
              Audience.get(event.getPlayer()).sendWarning(mute.getSignMuteMessage());
            });
  }

  // BANS
  @Nullable
  public Cache<UUID, Punishment> getMatchBans() {
    return matchBan;
  }

  protected void addBan(UUID playerId, Punishment punishment) {
    if (getModerationConfig().isObservingBan()) {
      observerBanCache.put(playerId, punishment);
      logger.info("Cached observing ban for " + playerId.toString());
    }
  }

  protected void removeCachedBan(UUID playerId) {
    banEvasionCache.invalidate(playerId);
    observerBanCache.invalidate(playerId);
    pardonedPlayers.put(playerId, Instant.now());

    if (Bukkit.getPlayer(playerId) != null && getModerationConfig().isObservingBan()) {
      integration.updateBanPrefix(Bukkit.getPlayer(playerId), false);
    }
  }

  public Optional<Punishment> getOnlineBan(UUID playerId) {
    return Optional.ofNullable(observerBanCache.getIfPresent(playerId));
  }

  @Override
  public boolean isServerSpaceAvaiable() {
    return (getModerationConfig().getMaxOnlineBans() - getOnlineBanCount()) > 0;
  }

  public int getOnlineBanCount() {
    return Math.toIntExact(
        observerBanCache.asMap().keySet().stream()
            .filter(id -> Bukkit.getPlayer(id) != null)
            .count());
  }

  // MUTES
  protected void addMute(UUID playerId, MutePunishment punishment) {
    muteCache.put(playerId, punishment);
  }

  protected void removeMute(UUID playerId) {
    muteCache.invalidate(playerId);
  }

  public Optional<MutePunishment> getCachedMute(UUID playerId) {
    MutePunishment mute = muteCache.getIfPresent(playerId);
    if (mute != null && !mute.isActive()) {
      muteCache.invalidate(playerId);
      return Optional.empty();
    }
    return Optional.ofNullable(mute);
  }

  // ETC.
  private Optional<UUID> getSenderId(CommandSender sender) {
    return Optional.ofNullable(sender instanceof Player ? ((Player) sender).getUniqueId() : null);
  }

  private Optional<UUID> isBanEvasion(String address) {
    Optional<Entry<UUID, Set<String>>> cached =
        banEvasionCache.asMap().entrySet().stream()
            .filter(s -> s.getValue().contains(address))
            .findAny();
    return Optional.ofNullable(cached.isPresent() ? cached.get().getKey() : null);
  }

  private boolean hasRecentPardon(UUID playerId) {
    return pardonedPlayers.getIfPresent(playerId) != null;
  }

  private void banHover() {
    color = !color;
    NamedTextColor alertColor = color ? NamedTextColor.YELLOW : NamedTextColor.DARK_RED;
    Component warning = text(" \u26a0 ", alertColor);
    Component banned =
        text()
            .append(warning)
            .append(text("You have been banned", NamedTextColor.RED, TextDecoration.BOLD))
            .append(warning)
            .build();

    this.observerBanCache.asMap().keySet().stream()
        .filter(id -> Bukkit.getPlayer(id) != null)
        .map(Bukkit::getPlayer)
        .map(Audience::get)
        .forEach(
            viewer -> {
              viewer.sendActionBar(banned);
            });
  }

  private void broadcastPunishment(
      Punishment punishment, boolean silent, @Nullable Audience audience) {
    broadcastPunishment(punishment, silent, null, audience);
  }

  private void broadcastPunishment(
      Punishment punishment, boolean silent, @Nullable String server, @Nullable Audience sender) {
    PunishmentFormats.formatBroadcast(punishment, server, getUsers())
        .thenAcceptAsync(
            broadcast -> {
              if (getModerationConfig().isBroadcasted()) { // Broadcast to global or staff
                if (silent || !getModerationConfig().isPunishmentPublic(punishment)) {
                  BroadcastUtils.sendAdminChatMessage(broadcast, server, null);
                } else {
                  BroadcastUtils.sendGlobalMessage(broadcast);
                }
              } else { // Send feedback if not broadcast
                Audience viewer = sender;
                if (viewer == null) {
                  viewer = Audience.console();
                }
                viewer.sendMessage(broadcast);
              }
            });
  }
}

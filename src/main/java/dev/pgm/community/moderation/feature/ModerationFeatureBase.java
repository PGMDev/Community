package dev.pgm.community.moderation.feature;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.util.text.TemporalComponent.duration;

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
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.text.PlayerComponent;
import tc.oc.pgm.events.PlayerParticipationStartEvent;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.TextTranslations;

public abstract class ModerationFeatureBase extends FeatureBase implements ModerationFeature {

  private final UsersFeature users;
  private final NetworkFeature network;
  private final Set<Punishment> recents;
  private final Cache<UUID, MutePunishment> muteCache;
  private final Cache<UUID, Set<String>> banEvasionCache;

  private Cache<UUID, Punishment> matchBan;

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
    this.banEvasionCache = CacheBuilder.newBuilder().build();

    if (config.getMatchBanDuration() != null) {
      this.matchBan =
          CacheBuilder.newBuilder()
              .expireAfterWrite(config.getMatchBanDuration().getSeconds(), TimeUnit.SECONDS)
              .build();
    }

    if (config.isEnabled()) {
      enable();

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

  /** Events * */
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
        users
            .getKnownIPs(punishment.getTargetId())
            .thenAcceptAsync(ips -> banEvasionCache.put(punishment.getTargetId(), ips));
        break;
      case MUTE: // Cache mute for easy lookup for sign/chat events
        muteCache.put(
            event.getPunishment().getTargetId(), MutePunishment.class.cast(event.getPunishment()));
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

  @EventHandler(priority = EventPriority.LOWEST)
  public void onPreLoginEvent(AsyncPlayerPreLoginEvent event) {
    this.onPreLogin(event);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerJoin(PlayerJoinEvent event) {
    String host = event.getPlayer().getAddress().getAddress().getHostAddress();
    Optional<UUID> banEvasion = isBanEvasion(host);

    if (banEvasion.isPresent()) {
      users
          .renderUsername(banEvasion, NameStyle.FANCY, null)
          .thenAcceptAsync(
              bannedName -> {
                BroadcastUtils.sendAdminChatMessage(
                    formatBanEvasion(event.getPlayer(), banEvasion.get(), bannedName),
                    Sounds.BAN_EVASION);
              });
    }
  }

  @EventHandler
  public void onMatchJoin(PlayerParticipationStartEvent event) {
    MatchPlayer player = event.getPlayer();
    if (matchBan != null) {
      Punishment punishment = matchBan.getIfPresent(player.getId());

      if (punishment != null) {
        Duration elasped = Duration.between(punishment.getTimeIssued(), Instant.now());
        Duration remaining = getModerationConfig().getMatchBanDuration().minus(elasped);
        Component reason =
            text()
                .append(text("You are banned from this match for "))
                .append(duration(remaining, NamedTextColor.YELLOW))
                .hoverEvent(
                    HoverEvent.showText(
                        text()
                            .append(text("Reason: ", NamedTextColor.AQUA))
                            .append(text(punishment.getReason(), NamedTextColor.GRAY))))
                .build();
        event.cancel(reason);
      }
    }
  }

  // Cancel chat for muted players
  @EventHandler(priority = EventPriority.HIGHEST)
  public void onAsyncPlayerChatEvent(AsyncPlayerChatEvent event) {
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

  protected void addMute(UUID playerId, MutePunishment punishment) {
    muteCache.put(playerId, punishment);
  }

  protected void removeMute(UUID playerId) {
    muteCache.invalidate(playerId);
  }

  protected void removeCachedBan(UUID playerId) {
    banEvasionCache.invalidate(playerId);
  }

  private Optional<MutePunishment> getCachedMute(UUID playerId) {
    MutePunishment mute = muteCache.getIfPresent(playerId);
    if (mute != null && !mute.isActive()) {
      muteCache.invalidate(playerId);
      return Optional.empty();
    }
    return Optional.ofNullable(mute);
  }

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

  private Component formatBanEvasion(Player player, UUID bannedId, Component banned) {
    return text()
        .append(
            translatable(
                "moderation.similarIP.loginEvent",
                NamedTextColor.GRAY,
                PlayerComponent.player(player, NameStyle.FANCY),
                banned))
        .hoverEvent(
            HoverEvent.showText(text("Click to issue ban evasion punishment", NamedTextColor.RED)))
        .clickEvent(
            ClickEvent.runCommand(
                "/ban "
                    + player.getName()
                    + " Ban Evasion - ("
                    + ChatColor.stripColor(TextTranslations.translateLegacy(banned, null) + ")")))
        .build();
  }
}

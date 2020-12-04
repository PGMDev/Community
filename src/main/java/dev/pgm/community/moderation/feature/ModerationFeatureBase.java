package dev.pgm.community.moderation.feature;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Sets;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.events.PlayerPunishmentEvent;
import dev.pgm.community.feature.FeatureBase;
import dev.pgm.community.moderation.ModerationConfig;
import dev.pgm.community.moderation.commands.BanCommand;
import dev.pgm.community.moderation.commands.KickCommand;
import dev.pgm.community.moderation.commands.MuteCommand;
import dev.pgm.community.moderation.commands.PunishmentCommand;
import dev.pgm.community.moderation.commands.WarnCommand;
import dev.pgm.community.moderation.punishments.Punishment;
import dev.pgm.community.moderation.punishments.PunishmentFormats;
import dev.pgm.community.moderation.punishments.PunishmentType;
import dev.pgm.community.moderation.punishments.types.MutePunishment;
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
import java.util.logging.Logger;
import java.util.stream.Collectors;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.event.HoverEvent;
import net.kyori.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import tc.oc.pgm.util.chat.Audience;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.types.PlayerComponent;

public abstract class ModerationFeatureBase extends FeatureBase implements ModerationFeature {

  private final UsersFeature users;
  private final Set<Punishment> recents;
  private final Cache<UUID, MutePunishment> muteCache;
  private final Cache<UUID, Set<String>> banEvasionCache;

  public ModerationFeatureBase(ModerationConfig config, Logger logger, UsersFeature users) {
    super(config, logger);
    this.recents = Sets.newHashSet();
    this.users = users;
    this.muteCache = CacheBuilder.newBuilder().build();
    this.banEvasionCache = CacheBuilder.newBuilder().build();

    if (config.isEnabled()) {
      enable();
    }
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
            getModerationConfig().getService(),
            getModerationConfig());
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

  /** Events * */
  @EventHandler(priority = EventPriority.LOWEST)
  public void onPunishmentEvent(PlayerPunishmentEvent event) {
    final Punishment punishment = event.getPunishment();

    recents.add(punishment); // Cache recent punishment

    punishment.punish(); // Perform the actual punishment

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
      default:
        break;
    }

    PunishmentFormats.formatBroadcast(punishment, getUsers())
        .thenAcceptAsync(
            broadcast -> {
              if (getModerationConfig().isBroadcasted()) { // Broadcast to global or staff
                if (event.isSilent() || !getModerationConfig().isPunishmentPublic(punishment)) {
                  BroadcastUtils.sendAdminChatMessage(broadcast);
                } else {
                  BroadcastUtils.sendGlobalMessage(broadcast);
                }
              } else { // Send feedback if not broadcast
                event.getSender().sendMessage(broadcast);
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
          .renderUsername(banEvasion)
          .thenAcceptAsync(
              bannedName -> {
                BroadcastUtils.sendAdminChatMessage(
                    formatBanEvasion(event.getPlayer(), banEvasion.get(), bannedName),
                    Sounds.BAN_EVASION);
              });
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
    return TextComponent.builder()
        .append(
            TranslatableComponent.of(
                "moderation.similarIP.loginEvent",
                TextColor.GRAY,
                PlayerComponent.of(player, NameStyle.FANCY),
                banned))
        .hoverEvent(
            HoverEvent.showText(
                TextComponent.of("Click to issue ban evasion punishment", TextColor.RED)))
        .clickEvent(ClickEvent.runCommand("/ban " + player.getName() + " Ban Evasion"))
        .build();
  }
}

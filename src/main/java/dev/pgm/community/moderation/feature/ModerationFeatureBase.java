package dev.pgm.community.moderation.feature;

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
import dev.pgm.community.moderation.punishments.PunishmentType;
import dev.pgm.community.moderation.punishments.types.MutePunishment;
import dev.pgm.community.usernames.UsernameService;
import dev.pgm.community.utils.BroadcastUtils;
import dev.pgm.community.utils.CommandAudience;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;
import net.kyori.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import tc.oc.pgm.util.chat.Audience;
import tc.oc.pgm.util.chat.Sound;

public abstract class ModerationFeatureBase extends FeatureBase implements ModerationFeature {

  private Set<Punishment> recents;
  private UsernameService usernames;

  public ModerationFeatureBase(ModerationConfig config, Logger logger, UsernameService usernames) {
    super(config, logger);
    this.recents = Sets.newHashSet();
    this.usernames = usernames;

    if (config.isEnabled()) {
      enable();
    }
  }

  public UsernameService getUsernames() {
    return usernames;
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
            getModerationConfig(),
            getUsernames());
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

  private Optional<UUID> getSenderId(CommandSender sender) {
    return Optional.ofNullable(sender instanceof Player ? ((Player) sender).getUniqueId() : null);
  }

  /** Events * */
  @EventHandler
  public void onPunishmentEvent(PlayerPunishmentEvent event) {
    recents.add(event.getPunishment()); // Cache recent punishment

    event.getPunishment().punish(); // Perform the actual punishment

    final Component broadcast = event.getPunishment().formatBroadcast(usernames);
    if (getModerationConfig().isBroadcasted()) { // Broadcast to global / staff
      if (event.isSilent()) {
        BroadcastUtils.sendAdminChat(broadcast, new Sound("item.fireCharge.use", 1f, 0.3f));
      } else {
        BroadcastUtils.sendGlobalChat(event.getPunishment().formatBroadcast(usernames));
      }
    } else { // Send feedback if not broadcast
      event.getSender().sendMessage(broadcast);
    }
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onPreLoginEvent(AsyncPlayerPreLoginEvent event) {
    this.onPreLogin(event);
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onAsyncPlayerChatEvent(AsyncPlayerChatEvent event) {
    try {
      Optional<Punishment> mute = isMuted(event.getPlayer().getUniqueId()).get();
      mute.map(MutePunishment.class::cast)
          .ifPresent(
              activeMute -> {
                event.setCancelled(true);
                Audience.get(event.getPlayer()).sendWarning(activeMute.getMuteMessage());
              });
    } catch (InterruptedException | ExecutionException e) {
      e.printStackTrace();
    }
  }
}

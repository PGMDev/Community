package dev.pgm.community.moderation.feature.types;

import com.google.common.collect.Lists;
import dev.pgm.community.Community;
import dev.pgm.community.moderation.ModerationConfig;
import dev.pgm.community.moderation.feature.ModerationFeatureBase;
import dev.pgm.community.moderation.punishments.Punishment;
import dev.pgm.community.moderation.punishments.PunishmentType;
import dev.pgm.community.moderation.punishments.types.MutePunishment;
import dev.pgm.community.moderation.services.SQLModerationService;
import dev.pgm.community.network.feature.NetworkFeature;
import dev.pgm.community.users.feature.UsersFeature;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result;
import tc.oc.pgm.util.named.NameStyle;

public class SQLModerationFeature extends ModerationFeatureBase {

  private SQLModerationService service;

  public SQLModerationFeature(
      Configuration config, Logger logger, UsersFeature usernames, NetworkFeature network) {
    super(new ModerationConfig(config), logger, "Punishments (SQL)", usernames, network);
    this.service = new SQLModerationService(getModerationConfig());
  }

  @Override
  public void save(Punishment punishment) {
    if (getModerationConfig().isPersistent()) {

      // When issuing a new ban or mute, check for existing and pardon if any.
      switch (punishment.getType()) {
        case TEMP_BAN:
        case BAN:
          isBanned(punishment.getTargetId().toString())
              .thenAcceptAsync(
                  banned -> {
                    if (banned) {
                      service
                          .pardon(punishment.getTargetId(), punishment.getIssuerId())
                          .thenAcceptAsync(x -> service.save(punishment));
                    } else {
                      service.save(punishment);
                    }
                  });
          break;
        case MUTE:
          isMuted(punishment.getTargetId())
              .thenAcceptAsync(
                  mute -> {
                    if (mute.isPresent()) {
                      service
                          .unmute(punishment.getTargetId(), punishment.getIssuerId())
                          .thenAcceptAsync(x -> service.save(punishment));
                    } else {
                      service.save(punishment);
                    }
                  });
          break;
        default:
          service.save(punishment);
          break;
      }
    }
  }

  @Override
  public CompletableFuture<List<Punishment>> query(String target) {
    if (UsersFeature.USERNAME_REGEX.matcher(target).matches()) {
      // CONVERT TO UUID if username
      return getUsers()
          .getStoredId(target)
          .thenApplyAsync(
              uuid ->
                  uuid != null && uuid.isPresent()
                      ? service.queryList(uuid.get().toString()).join()
                      : Lists.newArrayList());
    }
    return service.queryList(target);
  }

  @Override
  public CompletableFuture<Boolean> pardon(String target, Optional<UUID> issuer) {
    CompletableFuture<Optional<UUID>> playerId =
        UsersFeature.USERNAME_REGEX.matcher(target).matches()
            ? getUsers().getStoredId(target)
            : CompletableFuture.completedFuture(Optional.of(UUID.fromString(target)));
    return playerId.thenApplyAsync(
        uuid -> {
          if (uuid.isPresent()) {
            if (service.pardon(uuid.get(), issuer).join()) {
              sendRefresh(uuid.get());
              removeCachedBan(uuid.get());
              return true;
            }
          }
          return false;
        });
  }

  @Override
  public CompletableFuture<Boolean> isBanned(String target) {
    if (UsersFeature.USERNAME_REGEX.matcher(target).matches()) {
      return getUsers()
          .getStoredId(target)
          .thenApplyAsync(
              uuid -> uuid.isPresent() ? service.isBanned(uuid.get().toString()).join() : false);
    }
    return service.isBanned(target);
  }

  @Override
  public void onPreLogin(AsyncPlayerPreLoginEvent event) {
    List<Punishment> punishments;
    try {
      punishments =
          service
              .queryList(event.getUniqueId().toString())
              .get(getModerationConfig().getLoginTimeout(), TimeUnit.SECONDS);

      Optional<Punishment> ban = hasActiveBan(punishments);
      if (ban.isPresent()) {
        Punishment punishment = ban.get();
        event.setKickMessage(
            punishment.formatPunishmentScreen(
                getModerationConfig(),
                getUsers().renderUsername(punishment.getIssuerId(), NameStyle.FANCY).join(),
                false));
        event.setLoginResult(Result.KICK_BANNED);

        if (punishment.getType() == PunishmentType.NAME_BAN) {
          String bannedName = punishment.getReason();
          if (!event.getName().equalsIgnoreCase(bannedName)) {
            pardon(punishment.getTargetId().toString(), Optional.empty());
            event.setLoginResult(Result.ALLOWED);
            logger.info(
                String.format(
                    "Name change detected for (%s) | %s -> %s | Account unbanned",
                    punishment.getTargetId().toString(), punishment.getReason(), event.getName()));
          }
        }
      }

      Optional<MutePunishment> mute = hasActiveMute(punishments);
      if (mute.isPresent()) {
        addMute(event.getUniqueId(), mute.get());
      }

      logger.info(
          punishments.size()
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
    Community.get()
        .getServer()
        .getScheduler()
        .scheduleSyncDelayedTask(
            Community.get(),
            new Runnable() {
              @Override
              public void run() {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null) {
                  service
                      .queryList(playerId.toString())
                      .thenAcceptAsync(
                          punishments -> {
                            Optional<Punishment> ban = hasActiveBan(punishments);
                            if (ban.isPresent()) {
                              Punishment punishment = ban.get();

                              player.kickPlayer(
                                  punishment.formatPunishmentScreen(
                                      getModerationConfig(),
                                      getUsers()
                                          .renderUsername(punishment.getIssuerId(), NameStyle.FANCY)
                                          .join(),
                                      false));
                            }

                            Optional<MutePunishment> mute = hasActiveMute(punishments);
                            if (mute.isPresent()) {
                              addMute(playerId, mute.get());
                            }

                            logger.info(
                                "[Delayed]: "
                                    + punishments.size()
                                    + " Punishments have been fetched for "
                                    + playerId.toString());
                          });
                }
              }
            });
  }

  private Optional<MutePunishment> hasActiveMute(List<Punishment> punishments) {
    return punishments.stream()
        .filter(
            p ->
                p.isActive()
                    && p.getType().equals(PunishmentType.MUTE)
                    && p.getService().equalsIgnoreCase(getModerationConfig().getService()))
        .map(MutePunishment.class::cast)
        .findAny();
  }

  private Optional<Punishment> hasActiveBan(List<Punishment> punishments) {
    return punishments.stream()
        .filter(
            p ->
                p.isActive()
                    && p.getType().isLoginPrevented()
                    && p.getService().equalsIgnoreCase(getModerationConfig().getService()))
        .findAny();
  }

  @Override
  public CompletableFuture<Optional<Punishment>> isMuted(UUID target) {
    return service.isMuted(target);
  }

  @Override
  public CompletableFuture<Boolean> unmute(UUID id, Optional<UUID> issuer) {
    return service
        .unmute(id, issuer)
        .thenApplyAsync(
            success -> {
              if (success) {
                removeMute(id);
                sendRefresh(id); // Successful unmute will update other servers
              }
              return success;
            });
  }

  @Override
  public CompletableFuture<List<Punishment>> getRecentPunishments(Duration period) {
    return service.getRecentPunishments(period);
  }

  @Override
  public CompletableFuture<Integer> count() {
    return service.count();
  }

  @Override
  public void recieveRefresh(UUID playerId) {
    service.invalidate(playerId);
    removeCachedBan(playerId);
    removeMute(playerId);
  }
}

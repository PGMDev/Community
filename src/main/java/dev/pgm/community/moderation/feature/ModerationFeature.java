package dev.pgm.community.moderation.feature;

import dev.pgm.community.feature.Feature;
import dev.pgm.community.moderation.punishments.NetworkPunishment;
import dev.pgm.community.moderation.punishments.Punishment;
import dev.pgm.community.moderation.punishments.PunishmentType;
import dev.pgm.community.moderation.punishments.types.MutePunishment;
import dev.pgm.community.utils.CommandAudience;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

/** A feature that can handles moderation * */
public interface ModerationFeature extends Feature {

  /**
   * Punish a player the given punishment type
   *
   * @param type The type of punishment
   * @param target Who should be punished
   * @param issuer Who is issuing said punishment
   * @param reason Reason for the punishment
   * @param duration a length if needed (set to null if none)
   * @param active Whether punishment will be active
   * @param silent Whether punishment broadcast is silent
   * @return The {@link Punishment}
   */
  Punishment punish(
      PunishmentType type,
      UUID target,
      CommandAudience issuer,
      String reason,
      @Nullable Duration duration,
      boolean active,
      boolean silent);

  void save(Punishment punishment);

  /**
   * Query the backend to find all punishments for the given target
   *
   * @param target A username or UUID string
   * @return A future list of punishments
   */
  CompletableFuture<List<Punishment>> query(String target);

  /**
   * Pardon target for past punishments (Ban/Tempban)
   *
   * @param target A username or UUID string
   * @param issuer An optional UUID of the command sender (console is empty)
   * @return True if any ban infractions were lifted, false if none
   */
  CompletableFuture<Boolean> pardon(String target, Optional<UUID> issuer);

  /**
   * Get whether the target is currently banned
   *
   * @param target A username or UUID string
   * @return Whether target is banned
   */
  CompletableFuture<Boolean> isBanned(String target);

  /**
   * Gets active mute for the provided UUID, if any
   *
   * @param target A player UUID
   * @return An optional {@link Punishment}
   */
  CompletableFuture<Optional<Punishment>> isMuted(UUID target);

  /**
   * Unmutes any active mutes for the provided target
   *
   * @param target A player UUID
   * @param issuer The person lifting the infraction
   * @return true if unmute was removed, false if no mute existed
   */
  CompletableFuture<Boolean> unmute(UUID target, Optional<UUID> issuer);

  /**
   * Gets a set of online players who are muted
   *
   * @see {@link #isMuted(UUID)}
   * @return An set of players who are muted
   */
  Set<Player> getOnlineMutes();

  /**
   * Get a set of recent punishments during a time period
   *
   * @param period Time period to search
   * @return A set of punishments
   */
  CompletableFuture<List<Punishment>> getRecentPunishments(Duration period);

  /**
   * Get the last punishment a user issued Note: Used for /repeatpunishment
   *
   * @param issuer The command sender ID
   * @return The last punishment issued
   */
  Optional<Punishment> getLastPunishment(UUID issuer);

  Optional<MutePunishment> getCachedMute(UUID playerId);

  // AsyncPlayerPreLoginEvent handler
  void onPreLogin(AsyncPlayerPreLoginEvent event);

  // NETWORK STUFF
  void sendUpdate(NetworkPunishment punishment);

  void sendRefresh(UUID playerId);

  void recieveUpdate(NetworkPunishment punishment);

  void recieveRefresh(UUID playerId);
}

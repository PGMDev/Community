package dev.pgm.community.moderation.feature;

import dev.pgm.community.feature.Feature;
import dev.pgm.community.moderation.punishments.Punishment;
import dev.pgm.community.moderation.punishments.PunishmentType;
import dev.pgm.community.utils.CommandAudience;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

/** A feature that can handle punishments * */
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
   * @return Whether any punishments were pardoned
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
   * Get the most appropriate punishment based on history Warn -> Kick -> TempBan -> PermBan
   *
   * @param target A player id
   * @return The {@link PunishmentType} to issue
   */
  CompletableFuture<PunishmentType> getNextPunishment(UUID target);

  /**
   * Return a non-updated list of punishments that have been issued since server start.
   *
   * @return A set of {@link Punishment}
   */
  Set<Punishment> getRecentPunishments();

  /**
   * Get the last punishment a user issued Note: Used for /repeatpunishment
   *
   * @param issuer The command sender ID
   * @return The last punishment issued
   */
  Optional<Punishment> getLastPunishment(UUID issuer);

  // AsyncPlayerPreLoginEvent handler
  void onPreLogin(AsyncPlayerPreLoginEvent event);
}

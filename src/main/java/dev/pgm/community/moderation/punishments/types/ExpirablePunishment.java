package dev.pgm.community.moderation.punishments.types;

import dev.pgm.community.moderation.punishments.Punishment;
import dev.pgm.community.moderation.punishments.PunishmentType;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;

/** A punishment that can expire * */
public abstract class ExpirablePunishment extends Punishment {

  public ExpirablePunishment(
      PunishmentType type,
      UUID id,
      UUID targetId,
      Optional<UUID> issuerId,
      String reason,
      Instant timeIssued,
      Duration length,
      boolean active,
      Instant lastUpdated,
      Optional<UUID> lastUpdatedBy,
      String service) {
    super(
        type,
        id,
        targetId,
        issuerId,
        reason,
        length,
        timeIssued,
        active,
        lastUpdated,
        lastUpdatedBy,
        service);
  }

  @Override
  public boolean isActive() {
    Instant expires = this.getTimeIssued().plus(this.getDuration());
    return super.isActive()
        ? Instant.now().isBefore(expires)
        : false; // If expired return false, otherwise return true until expires
  }

  public Instant getExpireTime() {
    return getTimeIssued().plus(getDuration());
  }

  public static @Nullable Duration getDuration(Punishment punishment) {
    return punishment instanceof ExpirablePunishment
        ? ExpirablePunishment.class.cast(punishment).getDuration()
        : null;
  }
}

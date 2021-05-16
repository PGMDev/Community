package dev.pgm.community.moderation.punishments.types;

import dev.pgm.community.moderation.punishments.PunishmentType;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class TempBanPunishment extends ExpirablePunishment {

  public TempBanPunishment(
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
        PunishmentType.TEMP_BAN,
        id,
        targetId,
        issuerId,
        reason,
        timeIssued,
        length,
        active,
        lastUpdated,
        lastUpdatedBy,
        service);
  }

  @Override
  public boolean punish(boolean silent) {
    return kick(silent);
  }
}

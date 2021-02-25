package dev.pgm.community.moderation.punishments.types;

import dev.pgm.community.moderation.ModerationConfig;
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
      String service,
      ModerationConfig config) {
    super(
        id,
        targetId,
        issuerId,
        reason,
        timeIssued,
        length,
        active,
        lastUpdated,
        lastUpdatedBy,
        service,
        config);
  }

  @Override
  public boolean punish(boolean silent) {
    return kick(silent);
  }

  @Override
  public PunishmentType getType() {
    return PunishmentType.TEMP_BAN;
  }
}

package dev.pgm.community.moderation.punishments.types;

import dev.pgm.community.moderation.ModerationConfig;
import dev.pgm.community.moderation.punishments.PunishmentBase;
import dev.pgm.community.moderation.punishments.PunishmentType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class KickPunishment extends PunishmentBase {

  public KickPunishment(
      UUID id,
      UUID targetId,
      Optional<UUID> issuerId,
      String reason,
      Instant timeIssued,
      boolean active,
      Instant lastUpdated,
      Optional<UUID> lastUpdatedBy,
      ModerationConfig config) {
    super(id, targetId, issuerId, reason, timeIssued, active, lastUpdated, lastUpdatedBy, config);
  }

  @Override
  public boolean punish() {
    return kick();
  }

  @Override
  public PunishmentType getType() {
    return PunishmentType.KICK;
  }
}

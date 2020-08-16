package dev.pgm.community.moderation.punishments.types;

import dev.pgm.community.moderation.ModerationConfig;
import dev.pgm.community.moderation.punishments.PunishmentType;
import dev.pgm.community.users.feature.UsersFeature;
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
      ModerationConfig config,
      UsersFeature usernames) {
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
        config,
        usernames);
  }

  @Override
  public boolean punish() {
    return kick();
  }

  @Override
  public PunishmentType getType() {
    return PunishmentType.TEMP_BAN;
  }
}

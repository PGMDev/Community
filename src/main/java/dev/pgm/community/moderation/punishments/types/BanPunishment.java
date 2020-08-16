package dev.pgm.community.moderation.punishments.types;

import dev.pgm.community.moderation.ModerationConfig;
import dev.pgm.community.moderation.punishments.PunishmentBase;
import dev.pgm.community.moderation.punishments.PunishmentType;
import dev.pgm.community.users.feature.UsersFeature;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class BanPunishment extends PunishmentBase {

  public BanPunishment(
      UUID punishmentId,
      UUID targetId,
      Optional<UUID> issuerId,
      String reason,
      Instant timeIssued,
      boolean active,
      Instant lastUpdated,
      Optional<UUID> lastUpdatedBy,
      ModerationConfig config,
      UsersFeature usernames) {
    super(
        punishmentId,
        targetId,
        issuerId,
        reason,
        timeIssued,
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
    return PunishmentType.BAN;
  }
}

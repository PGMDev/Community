package dev.pgm.community.moderation.punishments.types;

import dev.pgm.community.moderation.ModerationConfig;
import dev.pgm.community.moderation.punishments.PunishmentType;
import dev.pgm.community.usernames.UsernameService;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class MutePunishment extends ExpirablePunishment {

  public MutePunishment(
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
      UsernameService usernames) {
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
    return false;
  }

  @Override
  public PunishmentType getType() {
    return PunishmentType.MUTE;
  }
}

package dev.pgm.community.moderation.punishments.types;

import dev.pgm.community.moderation.punishments.Punishment;
import dev.pgm.community.moderation.punishments.PunishmentType;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

public class NotePunishment extends Punishment {

  public NotePunishment(
      UUID id,
      UUID targetId,
      @Nullable UUID issuerId,
      String reason,
      long timeIssued,
      boolean active,
      long lastUpdated,
      @Nullable UUID lastUpdatedBy,
      String service) {
    super(
        PunishmentType.NOTE,
        id,
        targetId,
        issuerId,
        reason,
        null,
        timeIssued,
        active,
        lastUpdated,
        lastUpdatedBy,
        service);
  }

  @Override
  public boolean punish(boolean silent) {
    return true;
  }
}

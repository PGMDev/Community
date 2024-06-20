package dev.pgm.community.moderation.punishments.types;

import dev.pgm.community.moderation.punishments.PunishmentType;
import java.time.Duration;
import java.util.UUID;
import javax.annotation.Nullable;

public class TempBanPunishment extends ExpirablePunishment {

  public TempBanPunishment(
      UUID id,
      UUID targetId,
      @Nullable UUID issuerId,
      String reason,
      long timeIssued,
      Duration length,
      boolean active,
      long lastUpdated,
      @Nullable UUID lastUpdatedBy,
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

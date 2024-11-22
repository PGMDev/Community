package dev.pgm.community.moderation.punishments.types;

import dev.pgm.community.moderation.punishments.Punishment;
import dev.pgm.community.moderation.punishments.PunishmentType;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

public class BanPunishment extends Punishment {

  public BanPunishment(
      UUID punishmentId,
      UUID targetId,
      @Nullable UUID issuerId,
      String reason,
      long timeIssued,
      boolean active,
      long lastUpdated,
      @Nullable UUID lastUpdatedBy,
      String service) {
    super(
        PunishmentType.BAN,
        punishmentId,
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
    return kick(silent);
  }
}

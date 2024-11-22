package dev.pgm.community.moderation.punishments.types;

import dev.pgm.community.moderation.punishments.Punishment;
import dev.pgm.community.moderation.punishments.PunishmentType;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

public class UsernameBanPunishment extends Punishment {

  public UsernameBanPunishment(
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
        PunishmentType.NAME_BAN,
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

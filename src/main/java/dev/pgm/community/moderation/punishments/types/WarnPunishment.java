package dev.pgm.community.moderation.punishments.types;

import dev.pgm.community.moderation.punishments.Punishment;
import dev.pgm.community.moderation.punishments.PunishmentType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.entity.Player;
import tc.oc.pgm.util.Audience;

public class WarnPunishment extends Punishment {

  public WarnPunishment(
      UUID id,
      UUID targetId,
      Optional<UUID> issuerId,
      String reason,
      Instant timeIssued,
      boolean active,
      Instant lastUpdated,
      Optional<UUID> lastUpdatedBy,
      String service) {
    super(
        PunishmentType.WARN,
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
    Optional<Player> target = getTargetPlayer();
    target.ifPresent(player -> sendWarning(Audience.get(player), getReason()));
    return target.isPresent();
  }
}

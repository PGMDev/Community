package dev.pgm.community.moderation.punishments.types;

import dev.pgm.community.moderation.ModerationConfig;
import dev.pgm.community.moderation.punishments.PunishmentBase;
import dev.pgm.community.moderation.punishments.PunishmentType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.entity.Player;
import tc.oc.pgm.util.Audience;

public class WarnPunishment extends PunishmentBase {

  public WarnPunishment(
      UUID id,
      UUID targetId,
      Optional<UUID> issuerId,
      String reason,
      Instant timeIssued,
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
        active,
        lastUpdated,
        lastUpdatedBy,
        service,
        config);
  }

  @Override
  public boolean punish(boolean silent) {
    Optional<Player> target = getTargetPlayer();
    target.ifPresent(player -> sendWarning(Audience.get(player), getReason()));
    return target.isPresent();
  }

  @Override
  public PunishmentType getType() {
    return PunishmentType.WARN;
  }
}

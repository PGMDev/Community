package dev.pgm.community.moderation.feature;

import dev.pgm.community.moderation.punishments.Punishment;
import dev.pgm.community.moderation.punishments.types.MutePunishment;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.integration.PunishmentIntegration;

public class PGMPunishmentIntegration implements PunishmentIntegration {

  private ModerationFeatureBase moderation;

  public PGMPunishmentIntegration(ModerationFeatureBase moderation) {
    this.moderation = moderation;
  }

  @Override
  public boolean isMuted(Player player) {
    return moderation.getCachedMute(player.getUniqueId()).isPresent()
        || moderation.getOnlineBan(player.getUniqueId()).isPresent();
  }

  @Override
  public String getMuteReason(Player player) {
    return moderation
        .getCachedMute(player.getUniqueId())
        .map(MutePunishment::getReason)
        .orElse(
            moderation.getOnlineBan(player.getUniqueId()).map(Punishment::getReason).orElse(null));
  }

  @Override
  public boolean isHidden(Player player) {
    return moderation.getOnlineBan(player.getUniqueId()).isPresent();
  }
}

package dev.pgm.community.network.subs.types;

import dev.pgm.community.moderation.feature.ModerationFeature;
import dev.pgm.community.network.Channels;
import dev.pgm.community.network.subs.NetworkSubscriber;
import java.util.UUID;
import java.util.logging.Logger;

/** RefreshPunishmentSubscriber - Invalidates punishment cache for unbans/unmutes */
public class RefreshPunishmentSubscriber extends NetworkSubscriber {

  private ModerationFeature moderation;

  public RefreshPunishmentSubscriber(
      ModerationFeature moderation, String networkId, Logger logger) {
    super(Channels.PUNISHMENT_UPDATE, networkId, logger);
    this.moderation = moderation;
  }

  @Override
  public void onReceiveUpdate(String data) {
    try {
      UUID playerId = UUID.fromString(data);
      moderation.recieveRefresh(playerId);
      logger.info(String.format("Refreshed punishment data for %s", data));
    } catch (IllegalArgumentException e) {
      logger.warning(
          String.format(
              "Invalid UUID (%s) recieved for message channel (%s)", Channels.PUNISHMENTS, data));
    }
  }
}

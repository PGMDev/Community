package dev.pgm.community.network.subs;

import dev.pgm.community.moderation.feature.ModerationFeature;
import dev.pgm.community.network.Channels;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * PunishmentSubscriber - When update is received, will refresh player (invalidate punishment cache)
 * *
 */
public class PunishmentSubscriber extends NetworkSubscriber {

  private ModerationFeature moderation;

  public PunishmentSubscriber(ModerationFeature moderation, String networkId, Logger logger) {
    super(Channels.PUNISHMENTS, networkId, logger);
    this.moderation = moderation;
  }

  @Override
  public void onReceiveUpdate(String data) {
    try {
      UUID playerId = UUID.fromString(data);
      moderation.invalidate(playerId);
    } catch (IllegalArgumentException e) {
      logger.warning(
          String.format(
              "Invalid UUID (%s) recieved for message channel (%s)", Channels.PUNISHMENTS, data));
    }
  }
}

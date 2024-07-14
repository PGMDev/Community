package dev.pgm.community.network.subs.types;

import dev.pgm.community.moderation.feature.ModerationFeature;
import dev.pgm.community.moderation.punishments.NetworkPunishment;
import dev.pgm.community.network.Channels;
import dev.pgm.community.network.subs.NetworkSubscriber;
import java.util.logging.Logger;

/** PunishmentSubscriber - Listens for {@link NetworkPunishment} */
public class PunishmentSubscriber extends NetworkSubscriber {

  private ModerationFeature moderation;

  public PunishmentSubscriber(ModerationFeature moderation, String networkId, Logger logger) {
    super(Channels.PUNISHMENTS, networkId, logger);
    this.moderation = moderation;
  }

  @Override
  public void onReceiveUpdate(String data) {
    NetworkPunishment punishment = gson.fromJson(data, NetworkPunishment.class);
    if (punishment != null) {
      moderation.recieveUpdate(punishment);
    }
  }
}

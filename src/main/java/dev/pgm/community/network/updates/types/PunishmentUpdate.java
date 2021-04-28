package dev.pgm.community.network.updates.types;

import dev.pgm.community.moderation.punishments.NetworkPunishment;
import dev.pgm.community.network.Channels;
import dev.pgm.community.network.updates.NetworkUpdateBase;

/** PunishmentUpdate - Called when a {@link Punishment} is issued */
public class PunishmentUpdate extends NetworkUpdateBase<NetworkPunishment> {

  public PunishmentUpdate(NetworkPunishment punishment) {
    super(punishment, Channels.PUNISHMENTS);
  }
}

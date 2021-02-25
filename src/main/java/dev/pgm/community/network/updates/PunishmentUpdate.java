package dev.pgm.community.network.updates;

import dev.pgm.community.network.Channels;
import java.util.UUID;

public class PunishmentUpdate implements NetworkUpdate {

  private final UUID playerId;

  public PunishmentUpdate(UUID playerId) {
    this.playerId = playerId;
  }

  @Override
  public String getChannel() {
    return Channels.PUNISHMENTS;
  }

  @Override
  public String getData() {
    return playerId.toString();
  }
}

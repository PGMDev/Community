package dev.pgm.community.network.updates.types;

import dev.pgm.community.network.Channels;
import dev.pgm.community.network.updates.NetworkUpdate;
import java.util.UUID;

/** RefreshPunishmentUpdate - Called on ban pardon or unmute */
public class RefreshPunishmentUpdate implements NetworkUpdate {

  private final UUID playerId;

  public RefreshPunishmentUpdate(UUID playerId) {
    this.playerId = playerId;
  }

  @Override
  public String getChannel() {
    return Channels.PUNISHMENT_UPDATE;
  }

  @Override
  public String getData() {
    return playerId.toString();
  }
}

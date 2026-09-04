package dev.pgm.community.network.updates.types;

import dev.pgm.community.network.Channels;
import dev.pgm.community.network.updates.NetworkUpdate;
import java.util.UUID;

/** FriendshipUpdate - Sent when a player's friendships change (request/accept/reject) */
public class FriendshipUpdate implements NetworkUpdate {

  private final UUID playerId;

  public FriendshipUpdate(UUID playerId) {
    this.playerId = playerId;
  }

  @Override
  public String getChannel() {
    return Channels.FRIENDSHIP_UPDATE;
  }

  @Override
  public String getData() {
    return playerId.toString();
  }
}

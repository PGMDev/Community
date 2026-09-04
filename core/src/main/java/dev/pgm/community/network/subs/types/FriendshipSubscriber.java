package dev.pgm.community.network.subs.types;

import dev.pgm.community.friends.feature.FriendshipFeature;
import dev.pgm.community.network.Channels;
import dev.pgm.community.network.subs.NetworkSubscriber;
import java.util.UUID;
import java.util.logging.Logger;

/** FriendshipSubscriber - Invalidates cached friendships when they change on another server */
public class FriendshipSubscriber extends NetworkSubscriber {

  private final FriendshipFeature friends;

  public FriendshipSubscriber(FriendshipFeature friends, String networkId, Logger logger) {
    super(Channels.FRIENDSHIP_UPDATE, networkId, logger);
    this.friends = friends;
  }

  @Override
  public void onReceiveUpdate(String data) {
    try {
      friends.receiveNetworkInvalidation(UUID.fromString(data));
    } catch (IllegalArgumentException e) {
      logger.warning(String.format(
          "Invalid UUID (%s) received for message channel (%s)", data, Channels.FRIENDSHIP_UPDATE));
    }
  }
}

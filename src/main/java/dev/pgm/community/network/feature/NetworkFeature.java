package dev.pgm.community.network.feature;

import dev.pgm.community.feature.Feature;
import dev.pgm.community.network.subs.NetworkSubscriber;
import dev.pgm.community.network.updates.NetworkUpdate;

/** NetworkFeature - Feature related to communicating across multiple servers (Bungee) * */
public interface NetworkFeature extends Feature {

  /**
   * Send a {@link NetworkUpdate} to publisher
   *
   * @param update The network update
   */
  void sendUpdate(NetworkUpdate update);

  /**
   * Register a new {@link NetworkSubscriber} Will listen to and consume updates from {@link
   * #sendUpdate(NetworkUpdate)}
   *
   * @param sub
   */
  void registerSubscriber(NetworkSubscriber sub);

  /**
   * Get the Network ID The network id is used to identify server where update originated from.
   *
   * <p>Note: Community will not consume updates which originate from the source server
   *
   * @return the network id
   */
  String getNetworkId();
}

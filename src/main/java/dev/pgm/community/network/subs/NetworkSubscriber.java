package dev.pgm.community.network.subs;

import com.google.gson.Gson;
import java.util.logging.Logger;
import redis.clients.jedis.JedisPubSub;

public abstract class NetworkSubscriber extends JedisPubSub {

  private String channel;
  private String networkId;
  protected Logger logger;
  protected Gson gson;

  public NetworkSubscriber(String channel, String networkId, Logger logger) {
    this.channel = channel;
    this.networkId = networkId;
    this.logger = logger;
    this.gson = new Gson();
  }

  public String getNetworkId() {
    return networkId;
  }

  public String getChannel() {
    return channel;
  }

  public abstract void onReceiveUpdate(String data);

  @Override
  public void onMessage(String channel, String msg) {
    if (channel.equalsIgnoreCase(getChannel())) {
      // Data format -> 'networkId;message'
      String[] parts = msg.split(";");
      if (parts.length == 2) {
        String id = parts[0];
        String data = parts[1];
        if (!getNetworkId().equalsIgnoreCase(id)) {
          this.onReceiveUpdate(data);
        }
      }
    }
  }
}

package dev.pgm.community.network.subs.types;

import dev.pgm.community.chat.network.NetworkChatFeature;
import dev.pgm.community.chat.network.NetworkChatMessage;
import dev.pgm.community.network.Channels;
import dev.pgm.community.network.subs.NetworkSubscriber;
import java.util.logging.Logger;

/** ChatSubscriber - Listens for {@link NetworkChatMessage} */
public class ChatSubscriber extends NetworkSubscriber {

  private NetworkChatFeature chat;

  public ChatSubscriber(NetworkChatFeature chat, String networkId, Logger logger) {
    super(Channels.CHAT, networkId, logger);
    this.chat = chat;
  }

  @Override
  public void onReceiveUpdate(String data) {
    NetworkChatMessage message = gson.fromJson(data, NetworkChatMessage.class);
    if (message != null) {
      chat.recieveUpdate(message);
    }
  }
}

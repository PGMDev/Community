package dev.pgm.community.network.updates.types;

import dev.pgm.community.chat.network.NetworkChatMessage;
import dev.pgm.community.network.Channels;
import dev.pgm.community.network.updates.NetworkUpdateBase;

/** ChatUpdate - Relay chat across the network. See {@link MatchPlayerChatEvent} */
public class ChatUpdate extends NetworkUpdateBase<NetworkChatMessage> {

  public ChatUpdate(NetworkChatMessage message) {
    super(message, Channels.CHAT);
  }
}

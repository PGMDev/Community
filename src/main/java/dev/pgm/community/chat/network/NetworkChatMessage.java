package dev.pgm.community.chat.network;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import tc.oc.pgm.api.player.event.MatchPlayerChatEvent;
import tc.oc.pgm.listeners.ChatDispatcher.Channel;
import tc.oc.pgm.util.text.TextTranslations;

public class NetworkChatMessage {

  private String message;
  private String sender;
  private String server;
  private Channel channel;

  public NetworkChatMessage(MatchPlayerChatEvent event, String server) {
    this.message = TextTranslations.toMinecraftGson(event.getMessage(), null);
    this.sender = TextTranslations.toMinecraftGson(event.getSender(), null);
    this.channel = event.getChannel();
    this.server = server;
  }

  public Component getMessage() {
    return GsonComponentSerializer.colorDownsamplingGson().deserialize(message);
  }

  public Component getSender() {
    return GsonComponentSerializer.colorDownsamplingGson().deserialize(sender);
  }

  public String getServer() {
    return server;
  }

  public Channel getChannel() {
    return channel;
  }
}

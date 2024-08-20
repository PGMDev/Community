package dev.pgm.community.chat.network;

import static net.kyori.adventure.text.Component.text;
import static tc.oc.pgm.util.player.PlayerComponent.player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.channels.Channel;
import tc.oc.pgm.util.event.ChannelMessageEvent;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.TextTranslations;

public class NetworkChatMessage {

  private String message;
  private String sender;
  private String server;
  private Channel channel;

  public NetworkChatMessage(ChannelMessageEvent event, String server) {
    this.message = toMinecraftGson(text(event.getMessage()));
    this.sender = toMinecraftGson(player(event.getSender(), NameStyle.FANCY));
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

  private static String toMinecraftGson(Component component) {
    Component translated = TextTranslations.translate(component, Audience.empty());
    return GsonComponentSerializer.colorDownsamplingGson().serialize(translated);
  }
}

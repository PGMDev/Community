package dev.pgm.community.chat.network;

import static net.kyori.adventure.text.Component.text;
import static tc.oc.pgm.util.player.PlayerComponent.player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import tc.oc.pgm.api.event.ChannelMessageEvent;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.TextTranslations;

public class NetworkChatMessage {

  private final String message;
  private final String sender;
  private final String server;
  private final String channel;

  public NetworkChatMessage(ChannelMessageEvent<?> event, String server) {
    this.message = toMinecraftGson(text(event.getMessage()));
    this.sender = toMinecraftGson(player(event.getSender(), NameStyle.FANCY));
    this.channel = event.getChannel().getDisplayName();
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

  public String getChannel() {
    return channel;
  }

  private static String toMinecraftGson(Component component) {
    Component translated = TextTranslations.translate(component, Audience.empty());
    return GsonComponentSerializer.colorDownsamplingGson().serialize(translated);
  }
}

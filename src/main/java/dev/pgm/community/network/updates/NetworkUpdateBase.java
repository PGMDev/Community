package dev.pgm.community.network.updates;

import com.google.gson.Gson;
import dev.pgm.community.utils.gson.GsonProvider;

public abstract class NetworkUpdateBase<T> implements NetworkUpdate {

  private T item;
  private String channel;
  private Gson gson;

  public NetworkUpdateBase(T item, String channel) {
    this.item = item;
    this.channel = channel;
    this.gson = GsonProvider.get();
  }

  @Override
  public String getChannel() {
    return channel;
  }

  @Override
  public String getData() {
    return gson.toJson(item);
  }
}

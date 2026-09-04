package dev.pgm.community.network.updates.types;

import dev.pgm.community.network.Channels;
import dev.pgm.community.network.updates.NetworkUpdate;
import java.util.UUID;

public class SettingUpdate implements NetworkUpdate {

  private final UUID playerId;

  public SettingUpdate(UUID playerId) {
    this.playerId = playerId;
  }

  @Override
  public String getChannel() {
    return Channels.SETTING_UPDATE;
  }

  @Override
  public String getData() {
    return playerId.toString();
  }
}

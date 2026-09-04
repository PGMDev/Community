package dev.pgm.community.network.subs.types;

import dev.pgm.community.network.Channels;
import dev.pgm.community.network.subs.NetworkSubscriber;
import dev.pgm.community.settings.feature.SettingsFeature;
import java.util.UUID;
import java.util.logging.Logger;

public class SettingSubscriber extends NetworkSubscriber {

  private final SettingsFeature settings;

  public SettingSubscriber(SettingsFeature settings, String networkId, Logger logger) {
    super(Channels.SETTING_UPDATE, networkId, logger);
    this.settings = settings;
  }

  @Override
  public void onReceiveUpdate(String data) {
    try {
      settings.receiveNetworkInvalidation(UUID.fromString(data));
    } catch (IllegalArgumentException e) {
      logger.warning(String.format(
          "Invalid UUID (%s) received for message channel (%s)", data, Channels.SETTING_UPDATE));
    }
  }
}

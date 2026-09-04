package dev.pgm.community.settings.feature;

import dev.pgm.community.feature.Feature;
import dev.pgm.community.settings.CommunitySetting;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface SettingsFeature extends Feature {

  CompletableFuture<Map<String, String>> getSettings(UUID playerId);

  CompletableFuture<String> getValue(UUID playerId, CommunitySetting setting);

  default CompletableFuture<Boolean> isSettingEnabled(UUID playerId, CommunitySetting setting) {
    return getValue(playerId, setting).thenApply(Boolean::parseBoolean);
  }

  boolean isSettingEnabledCached(UUID playerId, CommunitySetting setting);

  CompletableFuture<Void> setValue(UUID playerId, CommunitySetting setting, String value);

  CompletableFuture<Boolean> toggle(UUID playerId, CommunitySetting setting);

  void receiveNetworkInvalidation(UUID playerId);
}

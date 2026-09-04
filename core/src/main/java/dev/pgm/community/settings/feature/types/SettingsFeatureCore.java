package dev.pgm.community.settings.feature.types;

import dev.pgm.community.feature.FeatureBase;
import dev.pgm.community.network.feature.NetworkFeature;
import dev.pgm.community.network.subs.types.SettingSubscriber;
import dev.pgm.community.network.updates.types.SettingUpdate;
import dev.pgm.community.settings.CommunitySetting;
import dev.pgm.community.settings.SettingsConfig;
import dev.pgm.community.settings.UserSetting;
import dev.pgm.community.settings.feature.SettingsFeature;
import dev.pgm.community.settings.store.SettingsStore;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

public class SettingsFeatureCore extends FeatureBase implements SettingsFeature {

  private final SettingsStore store;
  private final NetworkFeature network;

  public SettingsFeatureCore(
      Configuration config, Logger logger, SettingsStore store, NetworkFeature network) {
    super(new SettingsConfig(config), logger, "Settings");
    this.store = store;
    this.network = network;

    if (getConfig().isEnabled()) {
      enable();
      network.registerSubscriber(new SettingSubscriber(this, network.getNetworkId(), logger));
    }
  }

  @Override
  public CompletableFuture<Map<String, String>> getSettings(UUID playerId) {
    // Feature disabled = everyone at defaults, no DB calls
    if (!isEnabled()) {
      return CompletableFuture.completedFuture(Map.of());
    }
    return store
        .queryList(playerId)
        .thenApplyAsync(settings ->
            settings.stream().collect(Collectors.toMap(UserSetting::key, UserSetting::value)));
  }

  @Override
  public CompletableFuture<String> getValue(UUID playerId, CommunitySetting setting) {
    return getSettings(playerId)
        .thenApply(values -> values.getOrDefault(setting.getKey(), setting.getDefaultValue()));
  }

  @Override
  public boolean isSettingEnabledCached(UUID playerId, CommunitySetting setting) {
    if (!isEnabled()) return Boolean.parseBoolean(setting.getDefaultValue());
    return store
        .getCachedValue(playerId, setting.getKey())
        .map(Boolean::parseBoolean)
        .orElseGet(() -> Boolean.parseBoolean(setting.getDefaultValue()));
  }

  @Override
  public CompletableFuture<Void> setValue(UUID playerId, CommunitySetting setting, String value) {
    if (!isEnabled()) return CompletableFuture.completedFuture(null);
    return store
        .save(new UserSetting(playerId, setting.getKey(), value))
        .thenAccept(ignored -> network.sendUpdate(new SettingUpdate(playerId)));
  }

  @Override
  public void receiveNetworkInvalidation(UUID playerId) {
    store.invalidate(playerId);
    if (Bukkit.getPlayer(playerId) != null) {
      getSettings(playerId);
    }
  }

  @Override
  public CompletableFuture<Boolean> toggle(UUID playerId, CommunitySetting setting) {
    return isSettingEnabled(playerId, setting).thenCompose(enabled -> {
      boolean newValue = !enabled;
      return setValue(playerId, setting, Boolean.toString(newValue)).thenApply(ignored -> newValue);
    });
  }

  @EventHandler
  public void onAsyncLogin(AsyncPlayerPreLoginEvent event) {
    getSettings(event.getUniqueId());
  }
}

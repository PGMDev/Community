package dev.pgm.community.settings.store;

import dev.pgm.community.settings.UserSetting;
import dev.pgm.community.settings.services.SQLUserSettingsService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SQLSettingsStore implements SettingsStore {

  private final SQLUserSettingsService service;

  public SQLSettingsStore() {
    this.service = new SQLUserSettingsService();
  }

  @Override
  public CompletableFuture<Integer> save(UserSetting setting) {
    return service.saveAsync(setting);
  }

  @Override
  public void invalidate(UUID playerId) {
    service.invalidate(playerId);
  }

  @Override
  public Optional<String> getCachedValue(UUID playerId, String key) {
    return service.getCachedValue(playerId, key);
  }

  @Override
  public CompletableFuture<List<UserSetting>> queryList(UUID playerId) {
    return service.queryList(playerId.toString());
  }
}

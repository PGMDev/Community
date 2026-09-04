package dev.pgm.community.settings.store;

import dev.pgm.community.settings.UserSetting;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface SettingsStore {

  CompletableFuture<Integer> save(UserSetting setting);

  void invalidate(UUID playerId);

  Optional<String> getCachedValue(UUID playerId, String key);

  CompletableFuture<List<UserSetting>> queryList(UUID playerId);
}

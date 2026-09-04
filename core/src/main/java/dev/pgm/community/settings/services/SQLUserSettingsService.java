package dev.pgm.community.settings.services;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Maps;
import dev.pgm.community.database.DatabaseExecutor;
import dev.pgm.community.feature.SQLFeatureBase;
import dev.pgm.community.settings.UserSetting;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class SQLUserSettingsService extends SQLFeatureBase<UserSetting, String>
    implements UserSettingsQuery {

  private final LoadingCache<UUID, PlayerSettings> settingsCache;

  public SQLUserSettingsService() {
    super(TABLE_NAME, TABLE_FIELDS);
    this.settingsCache = CacheBuilder.newBuilder()
        .maximumSize(1000)
        .expireAfterAccess(4, TimeUnit.HOURS)
        .build(CacheLoader.from(ignored -> new PlayerSettings()));
  }

  @Override
  public void save(UserSetting setting) {
    saveAsync(setting);
  }

  public CompletableFuture<Integer> saveAsync(UserSetting setting) {
    PlayerSettings cached = settingsCache.getIfPresent(setting.playerId());
    if (cached != null) {
      cached.getValues().put(setting.key(), setting.value());
    }

    return DatabaseExecutor.executeUpdateAsync(
        DatabaseExecutor.getDialect().upsertUserSettingQuery(),
        setting.playerId().toString(),
        setting.key(),
        setting.value());
  }

  @Override
  public CompletableFuture<List<UserSetting>> queryList(String target) {
    UUID playerId = UUID.fromString(target);
    PlayerSettings playerSettings = settingsCache.getUnchecked(playerId);

    if (playerSettings.isLoaded()) {
      return CompletableFuture.completedFuture(toList(playerId, playerSettings));
    }

    return DatabaseExecutor.queryAsync(
            SELECT_SETTINGS_QUERY,
            row ->
                new UserSetting(playerId, row.getString("setting"), row.getString("setting_value")),
            playerId.toString())
        .thenApplyAsync(results -> {
          if (results != null) {
            results.forEach(
                setting -> playerSettings.getValues().putIfAbsent(setting.key(), setting.value()));
          }
          playerSettings.setLoaded(true);
          return toList(playerId, playerSettings);
        });
  }

  @Override
  public CompletableFuture<UserSetting> query(String target) {
    return CompletableFuture.completedFuture(null); // Use queryList
  }

  public void invalidate(UUID playerId) {
    settingsCache.invalidate(playerId);
  }

  public Optional<String> getCachedValue(UUID playerId, String key) {
    PlayerSettings cached = settingsCache.getIfPresent(playerId);
    if (cached == null || !cached.isLoaded()) return Optional.empty();
    return Optional.ofNullable(cached.getValues().get(key));
  }

  private static List<UserSetting> toList(UUID playerId, PlayerSettings settings) {
    List<UserSetting> list = new ArrayList<>();
    settings.getValues().forEach((key, value) -> list.add(new UserSetting(playerId, key, value)));
    return list;
  }

  private static class PlayerSettings {
    private final Map<String, String> values = Maps.newConcurrentMap();
    private volatile boolean loaded;

    public Map<String, String> getValues() {
      return values;
    }

    public boolean isLoaded() {
      return loaded;
    }

    public void setLoaded(boolean loaded) {
      this.loaded = loaded;
    }
  }
}

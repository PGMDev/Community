package dev.pgm.community.usernames.types;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;
import dev.pgm.community.Community;
import dev.pgm.community.usernames.UsernameChangeListener;
import dev.pgm.community.usernames.UsernameService;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import javax.annotation.Nullable;

public class CachedUsernameService implements UsernameService {

  private static final int CACHE_LIMIT = 5000;

  protected final Logger logger;
  private Cache<UUID, String> cache;

  public CachedUsernameService(Logger logger) {
    this.logger = logger;
    this.cache = CacheBuilder.newBuilder().maximumSize(CACHE_LIMIT).build();
    // Auto register username change listener
    Community.get().registerListener(new UsernameChangeListener(this));
  }

  @Override
  public @Nullable String getUsername(UUID id) {
    return cache.getIfPresent(id);
  }

  @Override
  public Optional<UUID> getId(String username) {
    return cache.asMap().entrySet().stream()
        .filter(e -> e.getValue().equalsIgnoreCase(username))
        .map(e -> e.getKey())
        .findAny();
  }

  @Override
  public CompletableFuture<String> getStoredUsername(UUID id) {
    return CompletableFuture.completedFuture(getUsername(id));
  }

  @Override
  public CompletableFuture<Optional<UUID>> getStoredId(String name) {
    return CompletableFuture.completedFuture(getId(name));
  }

  @Override
  public void setName(UUID uuid, String name) {
    cache.put(uuid, name);
  }

  @Override
  public Map<UUID, String> getAllNamesDebug() {
    return cache.asMap();
  }

  @Override
  public CompletableFuture<Map<UUID, String>> getStoredNamesDebug() {
    return CompletableFuture.completedFuture(Maps.newHashMap());
  }
}

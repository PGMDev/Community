package dev.pgm.community.requests.feature.types;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import dev.pgm.community.requests.RequestConfig;
import dev.pgm.community.requests.RequestProfile;
import dev.pgm.community.requests.feature.RequestFeatureBase;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.bukkit.configuration.Configuration;
import org.bukkit.event.player.PlayerJoinEvent;

public class NoDBRequestFeature extends RequestFeatureBase {

  private LoadingCache<UUID, RequestProfile> cache =
      CacheBuilder.newBuilder()
          .build(
              new CacheLoader<UUID, RequestProfile>() {
                @Override
                public RequestProfile load(UUID key) throws Exception {
                  return new RequestProfile(key);
                }
              });

  public NoDBRequestFeature(Configuration config, Logger logger) {
    super(new RequestConfig(config), logger, "NoDb");
  }

  @Override
  public CompletableFuture<RequestProfile> getRequestProfile(UUID playerId) {
    return CompletableFuture.completedFuture(cache.getUnchecked(playerId));
  }

  @Override
  public RequestProfile getCached(UUID playerId) {
    return cache.getUnchecked(playerId);
  }

  @Override
  public void update(RequestProfile profile) {
    // no-op
  }

  @Override
  public CompletableFuture<RequestProfile> onLogin(PlayerJoinEvent event) {
    return getRequestProfile(event.getPlayer().getUniqueId());
  }
}

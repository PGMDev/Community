package dev.pgm.community.users.feature.types;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import dev.pgm.community.Community;
import dev.pgm.community.events.UserProfileLoadEvent;
import dev.pgm.community.feature.FeatureBase;
import dev.pgm.community.users.UserProfile;
import dev.pgm.community.users.UserProfileImpl;
import dev.pgm.community.users.UsersConfig;
import dev.pgm.community.users.feature.UsersFeature;
import dev.pgm.community.users.listeners.UserProfileLoginListener;
import dev.pgm.community.users.services.AddressHistoryService.LatestAddressInfo;
import dev.pgm.community.users.store.UserStore;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jspecify.annotations.Nullable;

public class UsersFeatureCore extends FeatureBase implements UsersFeature {

  private final Cache<UUID, String> names;
  private final Cache<UUID, UserProfile> profiles;
  private final UserStore store;

  public UsersFeatureCore(Configuration config, Logger logger, UserStore store) {
    super(new UsersConfig(config), logger, "Users");
    this.profiles = CacheBuilder.newBuilder().build();
    this.names = CacheBuilder.newBuilder().build();
    this.store = store;

    if (getConfig().isEnabled()) {
      enable();
    }
    Community.get().registerListener(new UserProfileLoginListener(this));
  }

  public UsersConfig getUsersConfig() {
    return (UsersConfig) getConfig();
  }

  @Override
  public @Nullable String getUsername(UUID id) {
    return names.getIfPresent(id);
  }

  @Override
  public Optional<UUID> getId(String username) {
    return names.asMap().entrySet().stream()
        .filter(e -> e.getValue().equalsIgnoreCase(username))
        .map(Map.Entry::getKey)
        .findAny();
  }

  @Override
  public UserProfile getProfile(UUID id) {
    return profiles.getIfPresent(id);
  }

  @Override
  public void setName(UUID id, String name) {
    names.put(id, name);
  }

  @Override
  public CompletableFuture<UserProfile> getStoredProfile(String query) {
    return store.getProfile(query);
  }

  @Override
  public CompletableFuture<UserProfile> getStoredProfile(UUID id) {
    UserProfile cached = getProfile(id);
    if (cached == null) {
      return store.getProfile(id).thenApplyAsync(profile -> {
        if (profile != null) {
          profiles.put(id, profile); // Cache profile
        }
        return profile;
      });
    }
    return CompletableFuture.completedFuture(cached);
  }

  @Override
  public CompletableFuture<String> getStoredUsername(UUID id) {
    String cached = getUsername(id);

    if (cached == null) {
      return store.getProfile(id).thenApplyAsync(profile -> {
        if (profile != null && profile.getUsername() != null) {
          this.setName(id, profile.getUsername());
        }
        return profile.getUsername();
      });
    }

    return CompletableFuture.completedFuture(cached);
  }

  @Override
  public CompletableFuture<Optional<UUID>> getStoredId(String username) {
    Optional<UUID> cached = getId(username);
    if (cached.isEmpty()) {
      return store.getProfile(username).thenApplyAsync(profile -> {
        UUID id = null;
        if (profile != null && profile.getId() != null) {
          this.setName(profile.getId(), profile.getUsername());
          id = profile.getId();
        }
        return Optional.ofNullable(id);
      });
    }

    return CompletableFuture.completedFuture(cached);
  }

  @Override
  public CompletableFuture<Set<String>> getKnownIPs(UUID playerId) {
    return store.getKnownIps(playerId);
  }

  @Override
  public CompletableFuture<Set<UUID>> getAlternateAccounts(UUID playerId) {
    return store.getAlternateAccounts(playerId);
  }

  @Override
  public CompletableFuture<LatestAddressInfo> getLatestAddress(UUID playerId) {
    return store.getLatestAddress(playerId);
  }

  @Override
  public void onLogin(PlayerJoinEvent event) {
    final Player player = event.getPlayer();
    final UUID id = player.getUniqueId();
    final String name = player.getName();
    final String address = player.getAddress().getHostString();
    setName(id, name); // Check for username update

    profiles.invalidate(player.getUniqueId());
    store.login(id, name, address).thenAcceptAsync(profile -> {
      profiles.put(id, profile);

      // Call profile load event
      Community.get()
          .getServer()
          .getScheduler()
          .runTask(
              Community.get(),
              () -> Bukkit.getPluginManager().callEvent(new UserProfileLoadEvent(profile)));
    }); // Login save
    store.trackIp(id, address); // Track IP
  }

  @Override
  public void saveImportedUser(UUID id, String username) {
    getStoredProfile(id).thenAcceptAsync(profile -> {
      if (profile == null) {
        UserProfile up = new UserProfileImpl(id, username);
        store.save(up);
        setName(id, username);
      }
    });
  }

  @Override
  public CompletableFuture<Integer> count() {
    return store.count();
  }
}

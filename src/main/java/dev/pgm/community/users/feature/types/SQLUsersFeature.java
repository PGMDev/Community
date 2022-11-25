package dev.pgm.community.users.feature.types;

import dev.pgm.community.Community;
import dev.pgm.community.events.UserProfileLoadEvent;
import dev.pgm.community.users.UserProfile;
import dev.pgm.community.users.UserProfileImpl;
import dev.pgm.community.users.UsersConfig;
import dev.pgm.community.users.feature.UsersFeatureBase;
import dev.pgm.community.users.services.AddressHistoryService;
import dev.pgm.community.users.services.AddressHistoryService.LatestAddressInfo;
import dev.pgm.community.users.services.SQLUserService;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;

public class SQLUsersFeature extends UsersFeatureBase {

  private SQLUserService service;
  private AddressHistoryService addresses;

  public SQLUsersFeature(Configuration config, Logger logger) {
    super(new UsersConfig(config), logger, "Users (SQL)");
    this.service = new SQLUserService();
    this.addresses = new AddressHistoryService();
  }

  @Override
  public CompletableFuture<UserProfile> getStoredProfile(String query) {
    return service.query(query);
  }

  @Override
  public CompletableFuture<UserProfile> getStoredProfile(UUID id) {
    UserProfile cached = super.getProfile(id);
    if (cached == null) {
      return service
          .query(id.toString())
          .thenApplyAsync(
              profile -> {
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
    String cached = super.getUsername(id);

    if (cached == null) {
      return service
          .query(id.toString())
          .thenApplyAsync(
              profile -> {
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
    Optional<UUID> cached = super.getId(username);
    if (!cached.isPresent()) {
      return service
          .query(username)
          .thenApplyAsync(
              profile -> {
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
    return addresses.getKnownIps(playerId);
  }

  @Override
  public CompletableFuture<Set<UUID>> getAlternateAccounts(UUID playerId) {
    return addresses.getAlternateAccounts(playerId);
  }

  @Override
  public CompletableFuture<LatestAddressInfo> getLatestAddress(UUID playerId) {
    return addresses.getLatestAddressInfo(playerId);
  }

  @Override
  public void onLogin(PlayerJoinEvent event) {
    final Player player = event.getPlayer();
    final UUID id = player.getUniqueId();
    final String name = player.getName();
    final String address = player.getAddress().getHostString();
    setName(id, name); // Check for username update

    profiles.invalidate(
        player.getUniqueId()); // Removed cached profile upon every login, so we get up to date info
    service
        .login(id, name, address)
        .thenAcceptAsync(
            profile -> {
              profiles.put(id, profile);

              // Call profile load event
              Community.get()
                  .getServer()
                  .getScheduler()
                  .runTask(
                      Community.get(),
                      () -> Bukkit.getPluginManager().callEvent(new UserProfileLoadEvent(profile)));
            }); // Login save
    addresses.trackIp(id, address); // Track IP
  }

  @Override
  public void saveImportedUser(UUID id, String username) {
    getStoredProfile(id)
        .thenAcceptAsync(
            profile -> {
              if (profile == null) {
                UserProfile up = new UserProfileImpl(id, username);
                service.save(up);
                setName(id, username);
              }
            });
  }

  @Override
  public CompletableFuture<Integer> count() {
    return service.count();
  }
}

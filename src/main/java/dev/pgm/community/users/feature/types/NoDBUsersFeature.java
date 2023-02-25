package dev.pgm.community.users.feature.types;

import com.google.common.collect.Sets;
import dev.pgm.community.users.UserProfile;
import dev.pgm.community.users.UserProfileImpl;
import dev.pgm.community.users.UsersConfig;
import dev.pgm.community.users.feature.UsersFeatureBase;
import dev.pgm.community.users.services.AddressHistoryService.LatestAddressInfo;
import dev.pgm.community.utils.NameUtils;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;

public class NoDBUsersFeature extends UsersFeatureBase {

  public NoDBUsersFeature(Configuration config, Logger logger) {
    super(new UsersConfig(config), logger, "Users (No Database)");
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
  public CompletableFuture<UserProfile> getStoredProfile(UUID id) {
    return CompletableFuture.completedFuture(getProfile(id));
  }

  @Override
  public CompletableFuture<UserProfile> getStoredProfile(String query) {
    return CompletableFuture.completedFuture(
        getProfile(
            NameUtils.isMinecraftName(query)
                ? getStoredId(query).getNow(Optional.empty()).orElseGet(null)
                : UUID.fromString(query)));
  }

  @Override
  public CompletableFuture<Set<String>> getKnownIPs(UUID id) {
    String address = "";
    Player bukkit = Bukkit.getPlayer(id);
    if (bukkit != null) address = bukkit.getAddress().getAddress().getHostAddress();
    return CompletableFuture.completedFuture(Sets.newHashSet(address));
  }

  @Override
  public CompletableFuture<Set<UUID>> getAlternateAccounts(UUID playerId) {
    return CompletableFuture.completedFuture(Sets.newHashSet()); // TODO
  }

  @Override
  public CompletableFuture<LatestAddressInfo> getLatestAddress(UUID playerId) {
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public void onLogin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    profiles.invalidate(player.getUniqueId());
    this.profiles.put(
        player.getUniqueId(), new UserProfileImpl(player.getUniqueId(), player.getName()));
    this.setName(player.getUniqueId(), player.getName());
  }

  @Override
  public void saveImportedUser(UUID id, String username) {
    setName(id, username);
  }
}

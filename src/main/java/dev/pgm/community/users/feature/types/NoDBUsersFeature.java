package dev.pgm.community.users.feature.types;

import com.google.common.collect.Sets;
import dev.pgm.community.Community;
import dev.pgm.community.users.UserProfile;
import dev.pgm.community.users.UserProfileImpl;
import dev.pgm.community.users.UsersConfig;
import dev.pgm.community.users.feature.UsersFeature;
import dev.pgm.community.users.feature.UsersFeatureBase;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

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
            UsersFeature.USERNAME_REGEX.matcher(query).matches()
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
    Set<UUID> alts = this.alternateAccounts.getIfPresent(playerId);
    if(alts != null) {
      return CompletableFuture.completedFuture(alts);
    } else {
      CompletableFuture<Set<UUID>> alreadyActiveAltsFuture = this.currentlyFetchingAlts.getIfPresent(playerId);
      if(alreadyActiveAltsFuture != null) {
        return alreadyActiveAltsFuture;
      }

      Community.log("WARN: Alternate accounts not found in cache, fetching manually");
      CompletableFuture<Set<UUID>> fetchedAlts = CompletableFuture.completedFuture(Sets.newHashSet()); //TODO
      fetchedAlts.thenAcceptAsync(s -> this.alternateAccounts.put(playerId, s));
      return fetchedAlts;
    }
  }

  @Override
  public void onLogin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    profiles.invalidate(player.getUniqueId());
    this.profiles.put(
        player.getUniqueId(), new UserProfileImpl(player.getUniqueId(), player.getName()));
    this.setName(player.getUniqueId(), player.getName());

    this.alternateAccounts.put(player.getUniqueId(), Sets.newHashSet()); //TODO
  }

  @Override
  public void onLogout(PlayerQuitEvent event) {
    // Noop
  }

  @Override
  public void saveImportedUser(UUID id, String username) {
    setName(id, username);
  }
}

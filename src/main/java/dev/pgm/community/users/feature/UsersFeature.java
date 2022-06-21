package dev.pgm.community.users.feature;

import dev.pgm.community.feature.Feature;
import dev.pgm.community.users.UserProfile;
import dev.pgm.community.users.UserProfileWithSessionCallback;
import dev.pgm.community.users.services.AddressHistoryService.LatestAddressInfo;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.event.player.PlayerJoinEvent;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.PlayerComponent;
import tc.oc.pgm.util.text.PlayerComponentProvider;

/**
 * UsersFeature
 *
 * <p>Features related to users
 */
public interface UsersFeature extends Feature {

  // Not sure if this was the right place to put this
  // But better than calling XMLUtils
  static final Pattern USERNAME_REGEX = Pattern.compile("[a-zA-Z0-9_]{1,16}");

  default CompletableFuture<Component> renderUsername(UUID userId, NameStyle style) {
    return renderUsername(Optional.ofNullable(userId), style);
  }
  /**
   * Render a player name by looking up cached value or using database stored name
   *
   * @param userId Optional UUID of player, empty will result in console name
   * @return The rendered name component of provided UUID
   */
  default CompletableFuture<Component> renderUsername(Optional<UUID> userId, NameStyle style) {
    if (!userId.isPresent())
      return CompletableFuture.completedFuture(PlayerComponentProvider.CONSOLE);
    return getStoredUsername(userId.get())
        .thenApplyAsync(
            name -> PlayerComponent.player(Bukkit.getPlayer(userId.get()), name, style));
  }

  /**
   * Gets the cached username.
   *
   * <p>Useful if you know username is most likely online
   *
   * @param id Player UUID
   * @return The stored username
   */
  @Nullable
  String getUsername(UUID id);

  /**
   * Gets an optional UUID for the matching username
   *
   * @param username Player username
   * @return An optional uuid
   */
  Optional<UUID> getId(String username);

  /**
   * Queries database for a matching username
   *
   * <p>If not found, will return null but will queue a remote resolve
   *
   * @param id Player UUID
   * @return Stored username or null if not found
   */
  CompletableFuture<String> getStoredUsername(UUID id);

  /**
   * Queries database for a matching UUID
   *
   * <p>Note: Will NOT attempt to resolve uuid if not found
   *
   * @param username Player name
   * @return An optional UUID
   */
  CompletableFuture<Optional<UUID>> getStoredId(String username);

  /**
   * Queries the database for a user profile by UUID
   *
   * @param id Player UUID
   * @return A user profile if found or null
   */
  CompletableFuture<UserProfile> getStoredProfile(UUID id);

  /**
   * Queries the database for a user profile by either username or UUID
   *
   * @param query Username or UUID string
   * @return A user profile if found or null
   */
  CompletableFuture<UserProfile> getStoredProfile(String query);

  /**
   * Gets a cached UserProfile for the given UUID
   *
   * @param id Player UUID
   * @return A cached UserProfile or null
   */
  @Nullable
  UserProfile getProfile(UUID id);

  /**
   * Gets a set of IP strings that the given player ID has been associated with
   *
   * @param playerId Player UUID
   * @return A set of IP strings
   */
  CompletableFuture<Set<String>> getKnownIPs(UUID playerId);

  /**
   * Gets the latest known address associated with the player id
   *
   * @param playerId Player UUID
   * @return {@link LatestAddressInfo} with the most recent address info
   */
  CompletableFuture<LatestAddressInfo> getLatestAddress(UUID playerId);

  /**
   * Gets a set of alternate account ids related to the target
   *
   * @param playerId Player UUID
   * @return A set of UUIDs belonging to alternate accounts
   */
  CompletableFuture<Set<UUID>> getAlternateAccounts(UUID playerId);

  /**
   * Updates the stored username for matching id
   *
   * @param id Player UUID
   * @param name Username
   */
  void setName(UUID id, String name);

  /* Events to be handled by FeatureImpls */

  void onLogin(PlayerJoinEvent login);

  void saveImportedUser(UUID id, String name);

  /**
   * Queries the database for a user profile by either username or UUID and gets its latest session
   *
   * @param target Player username
   * @param ignoreDisguised Whether the target's latest session should include disguised sessions
   * @param callback The callback to be ran with
   * @throws ExecutionException
   */
  default void findUserWithSession(
      String target, boolean ignoreDisguised, UserProfileWithSessionCallback callback) {
    CompletableFuture<UserProfile> profileFuture = getStoredProfile(target);
    profileFuture.thenAcceptBothAsync(
        profileFuture.thenApplyAsync(
            profile -> {
              if (profile == null) return null;

              return profile.getLatestSession(ignoreDisguised).join();
            }),
        (profile, session) -> callback.run(profile, session));
  }

  /**
   * Queries the database for a user profile by either username or UUID and gets its latest session
   *
   * @param id Player UUID
   * @param ignoreDisguised Whether the target's latest session should include disguised sessions
   * @param callback The callback to be ran with
   * @throws ExecutionException
   */
  default void findUserWithSession(
      UUID id, boolean ignoreDisguised, UserProfileWithSessionCallback callback) {
    CompletableFuture<UserProfile> profileFuture = getStoredProfile(id);
    profileFuture.thenAcceptBothAsync(
        profileFuture.thenApplyAsync(
            profile -> {
              if (profile == null) return null;

              return profile.getLatestSession(ignoreDisguised).join();
            }),
        (profile, session) -> callback.run(profile, session));
  }
}

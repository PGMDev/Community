package dev.pgm.community.usernames;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import net.kyori.text.Component;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.types.PlayerComponent;

public interface UsernameService {

  // Not sure if this was the right place to put this
  // But better than calling XMLUtils
  static final Pattern USERNAME_REGEX = Pattern.compile("[a-zA-Z0-9_]{1,16}");

  /**
   * Render a player's name if cached
   *
   * @param userId Optional UUID of player, empty will result in console name
   * @return The rendered name component of provided UUID
   */
  default Component renderUsername(Optional<UUID> userId) {
    if (!userId.isPresent()) return PlayerComponent.CONSOLE;

    if (getUsername(userId.get()) != null) {
      return PlayerComponent.of(userId.get(), getUsername(userId.get()), NameStyle.FANCY);
    }
    return PlayerComponent.UNKNOWN;
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
   * Updates the stored username for matching id
   *
   * @param id Player UUID
   * @param name Username
   */
  public void setName(UUID id, String name);

  /** ******************** Methods below are for debug only ********************** */
  public Map<UUID, String> getAllNamesDebug();

  public CompletableFuture<Map<UUID, String>> getStoredNamesDebug();
}

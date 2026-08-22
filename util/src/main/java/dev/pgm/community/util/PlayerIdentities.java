package dev.pgm.community.util;

import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.util.skin.Skin;

/**
 * Per-viewer identities: what name, display name and skin each viewer is shown for a player.
 *
 * <p>Write-only by design. Nothing needs to read an identity back; PGM asks the nick integration
 * what a viewer should see, and the modern implementation keeps whatever extra state its packet
 * handlers need to itself.
 */
@NullMarked
public interface PlayerIdentities {
  PlayerIdentities IDENTITIES = Platform.get(PlayerIdentities.class);

  int MAX_NAME_LENGTH = 16;

  /**
   * Set every part of a player's identity for one viewer. Passing a null value clears that specific
   * field.
   */
  void setIdentity(
      Player player,
      Player viewer,
      @Nullable String name,
      @Nullable String displayName,
      @Nullable Skin skin);

  /** Show this viewer the player's real identity */
  default void clearIdentity(Player player, Player viewer) {
    setIdentity(player, viewer, null, null, null);
  }

  /** Show every viewer this player's real identity */
  void clearIdentities(Player player);

  /**
   * Forget every identity this player has, as a subject and as a viewer, without telling anyone.
   * Called when a player disconnects, at which point the client state cleans itself up.
   */
  void forget(Player player);

  /** Restore every player's real identity to every viewer. Called when Community is disabled. */
  void clearAll();

  static void validateName(@Nullable String name) {
    if (name != null && name.length() > MAX_NAME_LENGTH)
      throw new IllegalArgumentException(
          "Fake player names are limited to " + MAX_NAME_LENGTH + " characters in length");
  }
}

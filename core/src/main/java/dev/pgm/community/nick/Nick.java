package dev.pgm.community.nick;

import java.time.Instant;
import java.util.UUID;

public interface Nick {

  /**
   * Get the nickname
   *
   * @return a nickname string
   */
  String getName();

  void setName(String name);

  /**
   * Get the owning player's ID
   *
   * @return player id
   */
  UUID getPlayerId();

  /**
   * Get the date in which nickname was set by the player
   *
   * @return a date
   */
  Instant getDateSet();

  /**
   * Get whether the nickname is enabled Note: This represents whether the user wants their nickname
   * applied upon login
   *
   * @return true if nickname is enabled, false if not
   */
  boolean isEnabled();

  /**
   * Set whether the nickname is enabled
   *
   * @param enabled True for enabled, false for not
   */
  void setEnabled(boolean enabled);

  default void clear() {
    setName("");
    setEnabled(false);
  }

  public static Nick of(UUID playerId, String nick) {
    return new NickImpl(playerId, nick, Instant.now(), true);
  }
}

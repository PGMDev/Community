package dev.pgm.community.nick;

import java.time.Instant;
import java.util.UUID;

public interface Nick {

  /**
   * Get the nickname record ID
   *
   * @return nickname record uuid
   */
  UUID getNickId();

  /**
   * Get the nickname
   *
   * @return a nickname string
   */
  String getNickName();

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
   * Get whether this nickname is still valid Note: Nicknames are valid if player has yet to clear
   * or change their name
   *
   * @return true if valid
   */
  boolean isValid();

  /**
   * Set whether nickname is valid
   *
   * @param valid True if valid, false if not
   */
  void setValid(boolean valid);

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

  public static Nick of(UUID playerId, String nick) {
    return new NickImpl(UUID.randomUUID(), playerId, nick, Instant.now(), true, true);
  }
}

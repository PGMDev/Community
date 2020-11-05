package dev.pgm.community.users;

import java.time.Instant;
import java.util.UUID;

/**
 * UserProfile
 *
 * <p>A profile that holds data related to a user
 */
public interface UserProfile {

  /**
   * Get the player's UUID
   *
   * @return player UUID
   */
  UUID getId();

  /**
   * Get the player's last known username Note: If username has changed, will be updated next time
   * they login
   *
   * @return player username
   */
  String getUsername();

  /**
   * Get the player's first login date
   *
   * @return Date of first login
   */
  Instant getFirstLogin();

  /**
   * Get the player's last login date Note: last login is updated at server login & logout
   *
   * @return Date of last login
   */
  Instant getLastLogin();

  /**
   * Get the player's number of server joins
   *
   * @return Player join count
   */
  int getJoinCount();

  /**
   * Get the name of the last known server
   *
   * @return Name of the last known server
   */
  String getServerName();

  /**
   * Set the player's username
   *
   * @param username Player's username
   * @see {@link Player#getName}
   */
  void setUsername(String username);

  /**
   * Sets the player's first login date
   *
   * @param now The time of login
   */
  void setFirstLogin(Instant now);

  /**
   * Sets the player's last login date
   *
   * @param now The time of login
   */
  void setLastLogin(Instant now);

  /** Increases the join count by 1 */
  void incJoinCount();
}

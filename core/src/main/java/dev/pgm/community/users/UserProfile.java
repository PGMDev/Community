package dev.pgm.community.users;

import dev.pgm.community.sessions.Session;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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
   * Get the player's session
   *
   * @return The player's session
   */
  CompletableFuture<Session> getLatestSession(boolean ignoreDisguisedSessions);

  /**
   * Get the player's number of server joins
   *
   * @return Player join count
   */
  int getJoinCount();

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

  /** Increases the join count by 1 */
  void incJoinCount();
}

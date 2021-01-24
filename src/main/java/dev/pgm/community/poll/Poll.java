package dev.pgm.community.poll;

import java.time.Duration;
import java.util.UUID;
import javax.annotation.Nullable;
import org.bukkit.entity.Player;

/** Poll - Holds information required for running a poll * */
public interface Poll {

  /**
   * Get text question
   *
   * @return the poll question
   */
  String getText();

  /**
   * Get the number of yes or no votes
   *
   * @param yes - Whether to fetch yes or no votes
   * @return an int count of yes or no votes
   */
  int getVoteTally(boolean yes);

  /**
   * Get the player executing the poll
   *
   * @return A player if online (or null)
   */
  @Nullable
  Player getExecutor();

  /**
   * Get the overall length of the poll
   *
   * @return The duration of poll
   */
  Duration getLength();

  /**
   * Get the length of time remaining
   *
   * @return The duration remaining of poll
   */
  Duration getTimeLeft();

  /**
   * Get whether the poll is active
   *
   * @return true if active, false if not
   */
  boolean isActive();

  /**
   * Get the outcome of the poll based on yes/no votes
   *
   * @return if yes votes are greater than no
   */
  boolean getOutcome();

  /**
   * Set the poll question text
   *
   * @param text The question text
   */
  void setText(String text);

  /**
   * Set the player id of who will execute poll
   *
   * @param playerId - ID of player
   */
  void setExecutor(UUID playerId);

  /**
   * Set the length of the poll
   *
   * @param length
   */
  void setLength(Duration length);

  /**
   * Casts a vote for the player
   *
   * @param player - Player casting vote
   * @param yes - whether vote is yes or no
   */
  void vote(Player player, boolean yes);

  /** Starts the poll */
  void start();

  /** Completes the poll */
  void complete();
}

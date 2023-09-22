package dev.pgm.community.polls;

import dev.pgm.community.polls.ending.EndAction;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public interface Poll {

  Component getQuestion();

  UUID getCreator();

  Instant getStartTime();

  Instant getEndTime();

  void setEndTime(Instant time);

  boolean isRunning();

  List<EndAction> getEndAction();

  boolean vote(Player player, String option);

  long getTotalVotes();

  PollThreshold getRequiredThreshold();

  Duration getTimeLeft();

  boolean hasVoted(Player player);

  void tallyVotes();

  void start();

  Component getVoteButtons(boolean compact);
}

package dev.pgm.community.polls;

import dev.pgm.community.polls.ending.EndAction;
import java.time.Instant;
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

  boolean vote(Player player, boolean option);

  long getTotalVotes();

  long getYesVotesCount();

  long getNoVotesCount();

  EndAction getEndAction();
}

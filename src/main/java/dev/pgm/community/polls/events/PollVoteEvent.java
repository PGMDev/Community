package dev.pgm.community.polls.events;

import dev.pgm.community.polls.Poll;
import org.bukkit.entity.Player;

public class PollVoteEvent extends PollEvent {

  private Player player;

  public PollVoteEvent(Player player, Poll poll) {
    super(poll);
    this.player = player;
  }

  public Player getPlayer() {
    return player;
  }
}

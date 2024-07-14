package dev.pgm.community.polls.events;

import dev.pgm.community.polls.Poll;

public class PollStartEvent extends PollEvent {

  public PollStartEvent(Poll poll) {
    super(poll);
  }
}

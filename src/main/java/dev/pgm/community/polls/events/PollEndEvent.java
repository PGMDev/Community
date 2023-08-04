package dev.pgm.community.polls.events;

import dev.pgm.community.polls.Poll;

public class PollEndEvent extends PollEvent {

  public PollEndEvent(Poll poll) {
    super(poll);
  }
}

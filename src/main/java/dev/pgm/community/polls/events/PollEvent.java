package dev.pgm.community.polls.events;

import dev.pgm.community.events.CommunityEvent;
import dev.pgm.community.polls.Poll;

public abstract class PollEvent extends CommunityEvent {

  private final Poll poll;

  public PollEvent(Poll poll) {
    this.poll = poll;
  }

  public Poll getPoll() {
    return poll;
  }
}

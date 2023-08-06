package dev.pgm.community.polls.types;

import dev.pgm.community.polls.PollThreshold;
import dev.pgm.community.polls.ending.EndAction;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import net.kyori.adventure.text.Component;

public class TimedPoll extends NormalPoll {
  private final Duration duration;

  public TimedPoll(
      Component question,
      UUID creator,
      PollThreshold threshold,
      EndAction action,
      Duration duration) {
    super(question, creator, threshold, action);
    this.duration = duration;
    calculateEndTime();
  }

  private void calculateEndTime() {
    setEndTime(getStartTime().plus(duration));
  }

  public Duration getTimeLeft() {
    Instant now = Instant.now();
    if (now.isBefore(getEndTime())) {
      return Duration.between(now, getEndTime());
    }
    return Duration.ZERO;
  }
}

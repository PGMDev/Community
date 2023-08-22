package dev.pgm.community.polls.types;

import static net.kyori.adventure.text.Component.text;

import dev.pgm.community.polls.Poll;
import dev.pgm.community.polls.PollComponents;
import dev.pgm.community.polls.PollThreshold;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import net.kyori.adventure.text.Component;

public abstract class BasicPoll implements Poll, PollComponents {

  private final Component question;
  private final UUID creator;
  private final Instant startTime;
  private Instant endTime;
  private final Duration duration;
  private final PollThreshold threshold;

  public BasicPoll(Component question, UUID creator, PollThreshold threshold, Duration duration) {
    this.question = question;
    this.creator = creator;
    this.duration = duration;
    this.threshold = threshold;
    this.startTime = Instant.now();
    this.calculateEndTime();
  }

  @Override
  public void start() {
    sendPollBroadcast(this);
  }

  @Override
  public Component getQuestion() {
    if (question == null) {
      return text("");
    }
    return question;
  }

  @Override
  public UUID getCreator() {
    return creator;
  }

  @Override
  public Instant getStartTime() {
    return startTime;
  }

  @Override
  public Instant getEndTime() {
    return endTime;
  }

  @Override
  public void setEndTime(Instant time) {
    this.endTime = time;
  }

  @Override
  public boolean isRunning() {
    return endTime == null || endTime.isAfter(Instant.now());
  }

  @Override
  public PollThreshold getRequiredThreshold() {
    return threshold;
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

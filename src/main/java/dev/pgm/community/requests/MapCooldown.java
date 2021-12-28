package dev.pgm.community.requests;

import java.time.Duration;
import java.time.Instant;

public class MapCooldown {
  private Instant endTime;
  private Duration matchLength;

  public MapCooldown(Instant endTime, Duration matchLength) {
    this.endTime = endTime;
    this.matchLength = matchLength;
  }

  public Instant getEndTime() {
    return endTime;
  }

  public boolean hasExpired() {
    return getTimeRemaining().isNegative();
  }

  public Duration getTimeRemaining() {
    Duration timeSince = Duration.between(endTime, Instant.now());
    return matchLength.minus(timeSince);
  }
}

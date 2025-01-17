package dev.pgm.community.requests;

import java.time.Duration;
import java.time.Instant;
import tc.oc.pgm.util.TimeUtils;

public class MapCooldown {
  private final Instant endsAt;

  public MapCooldown(Duration cooldown) {
    this.endsAt = Instant.now().plus(cooldown);
  }

  public boolean hasExpired() {
    return !getTimeRemaining().isPositive();
  }

  public Duration getTimeRemaining() {
    return TimeUtils.max(Duration.ZERO, Duration.between(Instant.now(), endsAt));
  }
}

package dev.pgm.community.nick.data;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class NickSelection {

  private List<String> names;
  private Instant lastRefresh;

  public NickSelection(List<String> names) {
    this.names = names;
    this.lastRefresh = Instant.now();
  }

  public List<String> getNames() {
    return names;
  }

  public boolean canRefresh() {
    return Duration.between(lastRefresh, Instant.now()).toHours() > 1;
  }

  public boolean isValid(String input) {
    return names.stream().anyMatch(s -> s.equalsIgnoreCase(input));
  }
}

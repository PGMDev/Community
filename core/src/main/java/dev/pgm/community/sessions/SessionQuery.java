package dev.pgm.community.sessions;

import java.util.UUID;

public class SessionQuery {

  private final UUID playerId; // the UUID of the player
  private final boolean ignoreDisguised; // do we want to find a disguised session?

  public SessionQuery(UUID playerId, boolean ignoreDisguised) {
    this.playerId = playerId;
    this.ignoreDisguised = ignoreDisguised;
  }

  public UUID getPlayerId() {
    return playerId;
  }

  public boolean ignoreDisguised() {
    return ignoreDisguised;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof SessionQuery)) return false;
    SessionQuery other = (SessionQuery) obj;

    return other.playerId.equals(playerId) && other.ignoreDisguised == ignoreDisguised;
  }

  @Override
  public int hashCode() {
    return playerId.hashCode() * 31 + (ignoreDisguised ? 1 : 0);
  }
}

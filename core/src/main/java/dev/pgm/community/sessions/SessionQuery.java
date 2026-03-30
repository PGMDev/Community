package dev.pgm.community.sessions;

import java.util.UUID;

/**
 * @param playerId the UUID of the player
 * @param ignoreDisguised do we want to find a disguised session?
 */
public record SessionQuery(UUID playerId, boolean ignoreDisguised) {

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof SessionQuery(UUID id, boolean disguised))) return false;

    return id.equals(playerId) && disguised == ignoreDisguised;
  }

  @Override
  public int hashCode() {
    return playerId.hashCode() * 31 + (ignoreDisguised ? 1 : 0);
  }
}

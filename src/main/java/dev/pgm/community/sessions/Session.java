package dev.pgm.community.sessions;

import dev.pgm.community.Community;
import java.time.Instant;
import java.util.UUID;

public class Session {

  private final UUID sessionId; // ID of session

  private final UUID playerId; // UUID of player
  private final boolean disguised; // Whether the player is disguised/vanished

  private final String serverName; // The server that this session was started on

  private final Instant startDate; // Date the session started
  private Instant endDate; // Date the session ended

  public Session(UUID playerId, boolean disguised) {
    this(
        UUID.randomUUID(), playerId, disguised, Community.get().getServerId(), Instant.now(), null);
  }

  public Session(
      UUID sessionId,
      UUID playerId,
      boolean disguised,
      String serverName,
      Instant startDate,
      Instant endDate) {
    this.sessionId = sessionId;
    this.playerId = playerId;
    this.disguised = disguised;
    this.serverName = serverName;
    this.startDate = startDate;
    this.endDate = endDate;
  }

  public UUID getSessionId() {
    return sessionId;
  }

  public UUID getPlayerId() {
    return playerId;
  }

  public boolean isDisguised() {
    return disguised;
  }

  public String getServerName() {
    return serverName;
  }

  public Instant getStartDate() {
    return startDate;
  }

  public Instant getEndDate() {
    return endDate;
  }

  public void setEndDate(Instant endDate) {
    this.endDate = endDate;
  }

  public boolean hasEnded() {
    return getEndDate() != null;
  }

  /**
   * Gets the most recent updated time for this session
   *
   * @return the end date if the session has ended, otherwise the start date.
   */
  public Instant getLatestUpdateDate() {
    return hasEnded() ? getEndDate() : getStartDate();
  }

  public boolean isOnThisServer() {
    return serverName.equals(Community.get().getServerConfig().getServerId());
  }
}

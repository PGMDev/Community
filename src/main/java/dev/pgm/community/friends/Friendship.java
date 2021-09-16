package dev.pgm.community.friends;

import java.time.Instant;
import java.util.UUID;

public class Friendship implements Comparable<Friendship> {

  private UUID friendshipId; // ID of friendship

  private UUID requesterId; // UUID of player who requested friendship
  private UUID requestedId; // UUID of target

  private Instant requestDate; // Date the original request was sent

  private Instant requestUpdated; // Date request status was last updated

  private FriendshipStatus status; // Status of friendship

  public Friendship(UUID requesterId, UUID requestedId) {
    this(
        UUID.randomUUID(),
        requesterId,
        requestedId,
        FriendshipStatus.PENDING,
        Instant.now(),
        Instant.now());
  }

  /**
   * A friendship is a relation between two users
   *
   * @param friendshipId - ID of friendship
   * @param requesterId - ID of player who requested
   * @param requestedId - ID of player who was requested
   * @param status - Current status of friendship
   * @param requestDate - Date request was sent
   * @param updateDate - Date of last update to friendship status
   */
  public Friendship(
      UUID friendshipId,
      UUID requesterId,
      UUID requestedId,
      FriendshipStatus status,
      Instant requestDate,
      Instant updateDate) {
    this.friendshipId = friendshipId;
    this.requesterId = requesterId;
    this.requestedId = requestedId;
    this.status = status;
    this.requestDate = requestDate;
    this.requestUpdated = updateDate;
  }

  /**
   * Get the friendship id
   *
   * @return the friendship id
   */
  public UUID getFriendshipId() {
    return friendshipId;
  }

  /**
   * Get the id of player who requested the friendship
   *
   * @return A player id
   */
  public UUID getRequesterId() {
    return requesterId;
  }

  /**
   * Get the id of the player who was requested
   *
   * @return A player id
   */
  public UUID getRequestedId() {
    return requestedId;
  }

  /**
   * Get the time when request was first sent
   *
   * @return time of request
   */
  public Instant getRequestDate() {
    return requestDate;
  }

  /**
   * Get the time when friendship status was last updated
   *
   * @return time of last update
   */
  public Instant getLastUpdated() {
    return requestUpdated;
  }

  /**
   * Get the {@link FriendshipStatus}
   *
   * @return The status of the friendship
   */
  public FriendshipStatus getStatus() {
    return status;
  }

  /**
   * Sets the friendship status
   *
   * @param status
   */
  public void setStatus(FriendshipStatus status) {
    this.status = status;
  }

  /**
   * Sets the date in which the friendship status was last updated
   *
   * @param time
   */
  public void setLastUpdated(Instant now) {
    this.requestUpdated = now;
  }

  /**
   * Gets whether the provided UUID is either the requester or requested
   *
   * @param playerId A player's ID
   * @return Whether provided id is involved in the friendship
   */
  public boolean isInvolved(UUID playerId) {
    return getRequesterId().equals(playerId) || getRequestedId().equals(playerId);
  }

  /**
   * Gets whether a pair of player IDs are both involved in the friendship
   *
   * @param playerId1 A player id
   * @param playerId2 A player id
   * @return Whether ids are involved
   */
  public boolean areInvolved(UUID playerId1, UUID playerId2) {
    return getRequesterId().equals(playerId1) && getRequestedId().equals(playerId2)
        || getRequesterId().equals(playerId2) && getRequestedId().equals(playerId1);
  }

  /**
   * Given a player id, will return the opposite user. Ex. If provided ID is that of the requester,
   * it will return the requested.
   *
   * @param playerId A player id
   * @return The opposite player's id
   */
  public UUID getOtherPlayer(UUID playerId) {
    return getRequesterId().equals(playerId) ? getRequestedId() : getRequesterId();
  }

  public static enum FriendshipStatus {
    PENDING, // No decision has been more
    ACCEPTED, // Requested has accepted
    REJECTED; // Requested has denied
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof Friendship)) return false;
    Friendship otherFriendship = (Friendship) other;
    return otherFriendship.getFriendshipId().equals(getFriendshipId());
  }

  @Override
  public int compareTo(Friendship o) {
    return -getRequestDate().compareTo(o.getRequestDate());
  }
}

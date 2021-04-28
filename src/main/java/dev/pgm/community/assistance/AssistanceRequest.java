package dev.pgm.community.assistance;

import java.time.Instant;
import java.util.UUID;

// Represents a request for staff help by a player
public class AssistanceRequest {

  private UUID senderId;
  private UUID targetId;
  private Instant time;
  private String reason;
  private String server;
  private RequestType type;

  public AssistanceRequest() {}

  public AssistanceRequest(
      UUID senderId, UUID targetId, Instant time, String reason, String server, RequestType type) {
    this.senderId = senderId;
    this.targetId = targetId;
    this.time = time;
    this.reason = reason;
    this.server = server;
    this.type = type;
  }

  public UUID getSenderId() {
    return senderId;
  }

  public UUID getTargetId() {
    return targetId;
  }

  public Instant getTime() {
    return time;
  }

  public String getReason() {
    return reason;
  }

  public String getServer() {
    return server;
  }

  public RequestType getType() {
    return type;
  }

  public static enum RequestType {
    PLAYER_HELP,
    REPORT;
  }

  @Override
  public String toString() {
    return String.format(
        "{target: %s, sender: %s, reason:%s, time:%s, server:%s, type: %s}",
        getTargetId().toString(),
        getSenderId().toString(),
        getReason(),
        getTime().toString(),
        getServer(),
        getType().name());
  }
}

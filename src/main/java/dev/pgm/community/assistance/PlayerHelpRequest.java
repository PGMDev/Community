package dev.pgm.community.assistance;

import java.time.Instant;
import java.util.UUID;
import org.bukkit.entity.Player;

public class PlayerHelpRequest {

  private UUID playerId;
  private String reason;
  private Instant timeRequested;

  public PlayerHelpRequest(Player player, String reason) {
    this(player.getUniqueId(), reason, Instant.now());
  }

  public PlayerHelpRequest(UUID playerId, String reason, Instant timeRequested) {
    this.playerId = playerId;
    this.reason = reason;
    this.timeRequested = timeRequested;
  }

  public UUID getPlayerId() {
    return playerId;
  }

  public String getReason() {
    return reason;
  }

  public Instant getTimeRequested() {
    return timeRequested;
  }
}

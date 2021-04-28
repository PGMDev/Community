package dev.pgm.community.assistance;

import java.time.Instant;
import java.util.UUID;
import org.bukkit.entity.Player;

public class PlayerHelpRequest extends AssistanceRequest {

  public PlayerHelpRequest(Player player, String reason, String server) {
    this(player.getUniqueId(), reason, Instant.now(), server);
  }

  public PlayerHelpRequest(UUID playerId, String reason, Instant time, String server) {
    super(playerId, playerId, time, reason, server, RequestType.PLAYER_HELP);
  }
}

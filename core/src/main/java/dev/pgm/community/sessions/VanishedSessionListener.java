package dev.pgm.community.sessions;

import dev.pgm.community.sessions.feature.SessionFeature;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import tc.oc.pgm.api.player.event.PlayerVanishEvent;

public class VanishedSessionListener implements Listener {

  private final SessionFeature sessions;

  public VanishedSessionListener(SessionFeature sessions) {
    this.sessions = sessions;
  }

  @EventHandler
  public void onVanish(PlayerVanishEvent event) {
    Player player = event.getPlayer().getBukkit();
    if (sessions.isPlayerJoining(player)) return;

    sessions
        .getLatestSession(player.getUniqueId(), false)
        .thenAcceptAsync(
            session -> {
              sessions.endSession(session);
              sessions.startSession(player);
            });
  }
}

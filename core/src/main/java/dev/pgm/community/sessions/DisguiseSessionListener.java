package dev.pgm.community.sessions;

import dev.pgm.community.sessions.feature.SessionFeature;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import tc.oc.pgm.api.event.NameDecorationChangeEvent;
import tc.oc.pgm.api.player.event.PlayerVanishEvent;

public class DisguiseSessionListener implements Listener {

  private final SessionFeature sessions;

  public DisguiseSessionListener(SessionFeature sessions) {
    this.sessions = sessions;
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onVanish(PlayerVanishEvent event) {
    sessions.updateDisguise(event.getPlayer().getBukkit());
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onNameDecorationChange(NameDecorationChangeEvent event) {
    if (event.getUUID() == null) return;
    Player player = Bukkit.getPlayer(event.getUUID());
    if (player != null) sessions.updateDisguise(player);
  }
}

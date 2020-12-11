package dev.pgm.community.users.listeners;

import dev.pgm.community.users.feature.UsersFeature;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class UserProfileLoginListener implements Listener {

  private UsersFeature users;

  public UserProfileLoginListener(UsersFeature users) {
    this.users = users;
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onLoginEvent(final PlayerJoinEvent event) {
    users.onLogin(event);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onLogoutEvent(PlayerQuitEvent event) {
    users.onLogout(event);
  }
}

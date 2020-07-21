package dev.pgm.community.usernames;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

public class UsernameChangeListener implements Listener {

  private UsernameService usernameService;

  public UsernameChangeListener(UsernameService usernameService) {
    this.usernameService = usernameService;
  }

  @EventHandler
  public void onPreLogin(final AsyncPlayerPreLoginEvent event) {
    usernameService.setName(event.getUniqueId(), event.getName());
  }
}

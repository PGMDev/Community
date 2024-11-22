package dev.pgm.community.users.listeners;

import dev.pgm.community.Community;
import dev.pgm.community.events.UserProfileLoadEvent;
import dev.pgm.community.users.UsersConfig;
import dev.pgm.community.users.feature.UsersFeature;
import dev.pgm.community.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class UserProfileLoginListener implements Listener {

  private UsersFeature users;

  public UserProfileLoginListener(UsersFeature users) {
    this.users = users;
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onLoginEvent(final PlayerJoinEvent event) {
    users.onLogin(event);
  }

  @EventHandler
  public void onPostLoginEvent(UserProfileLoadEvent event) {
    UsersConfig config = (UsersConfig) users.getConfig();
    Player player = Bukkit.getPlayer(event.getUser().getId());
    if (player == null) return;

    // Custom messages / commands executed when joins server for the first time
    if (event.getUser().getJoinCount() <= 1) {
      config
          .getFirstJoinCommands()
          .forEach(
              command -> {
                command =
                    command
                        .replace("%uuid%", player.getUniqueId().toString())
                        .replace("%name%", player.getName());
                boolean message = command.startsWith("!send");
                if (message) {
                  String msg = command.substring(5, command.length());
                  player.sendMessage(MessageUtils.format(msg));
                } else {
                  Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);
                  Community.log(
                      "&7First join command (&6%s&7) executed for &b%s", command, player.getName());
                }
              });
    }
  }
}

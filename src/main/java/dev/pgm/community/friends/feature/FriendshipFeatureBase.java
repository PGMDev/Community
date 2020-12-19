package dev.pgm.community.friends.feature;

import static net.kyori.adventure.text.Component.text;

import com.google.common.collect.Sets;
import dev.pgm.community.Community;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.feature.FeatureBase;
import dev.pgm.community.friends.FriendshipConfig;
import dev.pgm.community.friends.PGMFriendManager;
import dev.pgm.community.friends.commands.FriendshipCommand;
import dev.pgm.community.utils.BroadcastUtils;
import dev.pgm.community.utils.Sounds;
import java.util.Set;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.util.Audience;

public abstract class FriendshipFeatureBase extends FeatureBase implements FriendshipFeature {

  protected @Nullable PGMFriendManager pgmFriends;

  public FriendshipFeatureBase(Configuration config, Logger logger) {
    super(new FriendshipConfig(config), logger);

    if (getConfig().isEnabled()) {
      enable();
    }
  }

  public FriendshipConfig getFriendshipConfig() {
    return (FriendshipConfig) getConfig();
  }

  @Override
  public void enable() {
    super.enable();
    enablePGMSupport();
  }

  private void enablePGMSupport() {
    Plugin pgmPlugin = Bukkit.getServer().getPluginManager().getPlugin("PGM");
    if (pgmPlugin != null
        && pgmPlugin.isEnabled()
        && getFriendshipConfig().isIntegrationEnabled()) {
      pgmFriends = new PGMFriendManager();
      Bukkit.getScheduler()
          .scheduleSyncDelayedTask(
              Community.get(),
              new Runnable() {
                @Override
                public void run() {
                  PGM.get().getFriendRegistry().setProvider(pgmFriends);
                }
              });
    }
  }

  @Override
  public Set<CommunityCommand> getCommands() {
    return getConfig().isEnabled() ? Sets.newHashSet(new FriendshipCommand()) : Sets.newHashSet();
  }

  public void sendFriendRequestLoginMessage(Player player, int requestCount) {
    Component requestsMessage =
        BroadcastUtils.RIGHT_DIV
            .color(NamedTextColor.GOLD)
            .append(text(" You have "))
            .append(text(requestCount, NamedTextColor.DARK_AQUA, TextDecoration.BOLD))
            .append(text(" pending friend request" + (requestCount != 1 ? "s" : "")))
            .append(BroadcastUtils.LEFT_DIV.color(NamedTextColor.GOLD))
            .color(NamedTextColor.DARK_GREEN)
            .hoverEvent(
                HoverEvent.showText(
                    text("Click to view pending friend requests", NamedTextColor.GRAY)))
            .clickEvent(ClickEvent.runCommand("/friend requests"));

    Audience.get(player).sendMessage(requestsMessage);
    Audience.get(player).playSound(Sounds.FRIEND_REQUEST_LOGIN);
  }

  @EventHandler
  public void onDelayedPlayerJoin(PlayerJoinEvent event) {
    // Used to send online friend requests notifications AFTER all other login messages have been
    // sent
    Bukkit.getScheduler()
        .scheduleSyncDelayedTask(
            Community.get(),
            new Runnable() {
              @Override
              public void run() {
                onDelayedLogin(event);
              }
            },
            40L);
  }

  @EventHandler
  public void onAsyncLogin(AsyncPlayerPreLoginEvent event) {
    this.onPreLogin(event);
  }
}

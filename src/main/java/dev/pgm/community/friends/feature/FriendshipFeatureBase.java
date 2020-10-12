package dev.pgm.community.friends.feature;

import com.google.common.collect.Sets;
import dev.pgm.community.Community;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.feature.FeatureBase;
import dev.pgm.community.friends.FriendshipConfig;
import dev.pgm.community.friends.commands.FriendshipCommand;
import dev.pgm.community.utils.BroadcastUtils;
import dev.pgm.community.utils.Sounds;
import java.util.Set;
import java.util.logging.Logger;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.event.HoverEvent;
import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import tc.oc.pgm.util.chat.Audience;

public abstract class FriendshipFeatureBase extends FeatureBase implements FriendshipFeature {

  public FriendshipFeatureBase(Configuration config, Logger logger) {
    super(new FriendshipConfig(config), logger);

    if (getConfig().isEnabled()) {
      enable();
    }
  }

  @Override
  public Set<CommunityCommand> getCommands() {
    return getConfig().isEnabled() ? Sets.newHashSet(new FriendshipCommand()) : Sets.newHashSet();
  }

  public void sendFriendRequestLoginMessage(Player player, int requestCount) {
    Component requestsMessage =
        TextComponent.builder()
            .append(BroadcastUtils.RIGHT_DIV.color(TextColor.GOLD))
            .append(" You have ")
            .append(TextComponent.of(requestCount, TextColor.DARK_AQUA, TextDecoration.BOLD))
            .append(" pending friend request" + (requestCount != 1 ? "s" : ""))
            .append(BroadcastUtils.LEFT_DIV.color(TextColor.GOLD))
            .color(TextColor.DARK_GREEN)
            .hoverEvent(
                HoverEvent.showText(
                    TextComponent.of("Click to view pending friend requests", TextColor.GRAY)))
            .clickEvent(ClickEvent.runCommand("/friend requests"))
            .build();
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

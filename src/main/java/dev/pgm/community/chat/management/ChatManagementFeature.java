package dev.pgm.community.chat.management;

import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.text;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Sets;
import dev.pgm.community.Community;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.feature.FeatureBase;
import dev.pgm.community.utils.BroadcastUtils;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import tc.oc.pgm.util.Audience;

/** ChatManagementFeature - Chat safety feature, including slowmode, lockdown, and clear. * */
public class ChatManagementFeature extends FeatureBase {

  private boolean lockdown;
  private boolean slowmode;

  private Cache<UUID, String> lastMessageCache;

  public ChatManagementFeature(Configuration config, Logger logger) {
    super(new ChatManagementConfig(config), logger, "Chat Management");
    this.lastMessageCache = CacheBuilder.newBuilder().build();
    if (getConfig().isEnabled()) {
      enable();
    }
  }

  public ChatManagementConfig getChatConfig() {
    return (ChatManagementConfig) getConfig();
  }

  public void toggleLockdown(CommandSender sender) {
    setLockdown(!isLockdown());
    broadcastModeChange(
        text("Chat Lockdown", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD),
        getLockdownHover(),
        isLockdown());
  }

  public void toggleSlowmode(CommandSender sender) {
    setSlowmode(!isSlowmode());
    broadcastModeChange(
        text("Chat Slowmode", NamedTextColor.GOLD, TextDecoration.BOLD),
        getSlowmodeHover(),
        isSlowmode());
  }

  private void broadcastModeChange(Component modeName, Component hover, boolean enabled) {
    Component message =
        text()
            .append(modeName)
            .append(text(" has been "))
            .append(
                enabled
                    ? text("enabled", NamedTextColor.GREEN)
                    : text("disabled", NamedTextColor.RED))
            .color(NamedTextColor.GRAY)
            .hoverEvent(HoverEvent.showText(hover))
            .build();
    BroadcastUtils.sendGlobalWarning(message);
  }

  public boolean isLockdown() {
    return lockdown;
  }

  public boolean isSlowmode() {
    return slowmode;
  }

  public void setLockdown(boolean enabled) {
    this.lockdown = enabled;
  }

  public void setSlowmode(boolean enabled) {
    this.slowmode = enabled;
  }

  private Component getSlowmodeHover() {
    Component builder = text("Slowmode has been ");

    if (isSlowmode()) {
      builder
          .append(text("enabled", NamedTextColor.GREEN))
          .append(text(" in order to reduce spam."))
          .append(newline())
          .append(BroadcastUtils.BROADCAST_DIV.color(NamedTextColor.GOLD))
          .append(text("Current slowmode speed: "))
          .append(text(getChatConfig().getSlowmodeSpeed(), NamedTextColor.YELLOW))
          .append(formatSeconds(getChatConfig().getSlowmodeSpeed()));
    } else {
      builder
          .append(text("disabled", NamedTextColor.RED))
          .append(text(". There is no longer a chat speed restriction in place"));
    }
    return builder.color(NamedTextColor.GRAY);
  }

  private Component getLockdownHover() {
    return text(
        isLockdown()
            ? "Chat messages can not be sent at this time"
            : "Chat messages can now be sent",
        NamedTextColor.GRAY);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerChat(AsyncPlayerChatEvent event) {
    boolean exempt = event.getPlayer().hasPermission(CommunityPermissions.CHAT_MANAGEMENT);
    if (exempt) return;
    if (event.isAsynchronous()) return;

    Player sender = event.getPlayer();
    Audience viewer = Audience.get(sender);

    // Block repeated messages
    if (getChatConfig().isBlockRepeatedMessagesEnabled()) {
      String lastMsg = lastMessageCache.getIfPresent(sender.getUniqueId());
      if (lastMsg != null && lastMsg.equalsIgnoreCase(event.getMessage())) {
        viewer.sendWarning(text("This message is too similar to your last"));
        event.setCancelled(true);
        return;
      }
      lastMessageCache.put(sender.getUniqueId(), event.getMessage());
    }

    // Lockdown - Cancel ALL player chat, except staff
    if (isLockdown()) {
      viewer.sendWarning(text("The chat is currently locked"));
      event.setCancelled(true);
      return;
    }

    // Slowmode - Put chat on a cooldown basis, defined by seconds from config
    if (isSlowmode()) {
      if (getChatConfig().getLastMessageCache().getIfPresent(sender.getUniqueId()) == null) {
        getChatConfig().getLastMessageCache().put(sender.getUniqueId(), Instant.now());
      } else {
        Instant lastSent = getChatConfig().getLastMessageCache().getIfPresent(sender.getUniqueId());
        Duration timeSince = Duration.between(lastSent, Instant.now());

        if (timeSince.getSeconds() < getChatConfig().getSlowmodeSpeed()) {
          long seconds = (getChatConfig().getSlowmodeSpeed() - timeSince.getSeconds());

          Component cooldownMsg =
              text("Please wait ")
                  .append(text(seconds, NamedTextColor.RED, TextDecoration.BOLD))
                  .append(formatSeconds(seconds))
                  .append(text(" before sending another message"))
                  .color(NamedTextColor.GRAY);

          viewer.sendWarning(cooldownMsg);
          event.setCancelled(true);
        } else {
          getChatConfig().getLastMessageCache().put(sender.getUniqueId(), Instant.now());
        }
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerJoin(PlayerJoinEvent event) {
    if (!getChatConfig().isLoginAlertsEnabled()) return;
    Audience viewer = Audience.get(event.getPlayer());
    if (isLockdown()) {
      sendDelayedMessage(
          viewer,
          text("The chat has been locked.")
              .append(newline())
              .append(text("No messages can be sent at this time")));
    } else if (isSlowmode()) {
      sendDelayedMessage(
          viewer,
          text("Chat slowmode has been ")
              .append(text("enabled", NamedTextColor.GREEN))
              .append(text(" to reduce spam."))
              .append(newline())
              .append(text("You may send a chat message once every "))
              .append(text(getChatConfig().getSlowmodeSpeed(), NamedTextColor.YELLOW))
              .append(formatSeconds(getChatConfig().getSlowmodeSpeed()))
              .color(NamedTextColor.GRAY));
    }
  }

  private void sendDelayedMessage(final Audience viewer, final Component message) {
    Bukkit.getScheduler()
        .scheduleSyncDelayedTask(
            Community.get(),
            new Runnable() {
              @Override
              public void run() {
                viewer.sendWarning(message);
              }
            },
            30L);
  }

  @Override
  public Set<CommunityCommand> getCommands() {
    return getChatConfig().isEnabled()
        ? Sets.newHashSet(new ChatManagementCommand())
        : Sets.newHashSet();
  }

  private Component formatSeconds(long seconds) {
    return text(String.format(" second%s", seconds != 1 ? "s" : ""));
  }
}

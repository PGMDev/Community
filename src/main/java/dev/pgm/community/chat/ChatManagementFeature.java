package dev.pgm.community.chat;

import com.google.common.collect.Sets;
import dev.pgm.community.Community;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.feature.FeatureBase;
import dev.pgm.community.utils.BroadcastUtils;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.logging.Logger;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.event.HoverEvent;
import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import tc.oc.pgm.util.chat.Audience;

public class ChatManagementFeature extends FeatureBase {

  private boolean lockdown;
  private boolean slowmode;

  public ChatManagementFeature(Configuration config, Logger logger) {
    super(new ChatManagementConfig(config), logger);
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
        TextComponent.of("Chat Lockdown", TextColor.DARK_PURPLE, TextDecoration.BOLD),
        getLockdownHover(),
        isLockdown());
  }

  public void toggleSlowmode(CommandSender sender) {
    setSlowmode(!isSlowmode());
    broadcastModeChange(
        TextComponent.of("Chat Slowmode", TextColor.GOLD, TextDecoration.BOLD),
        getSlowmodeHover(),
        isSlowmode());
  }

  private void broadcastModeChange(Component modeName, Component hover, boolean enabled) {
    Component message =
        TextComponent.builder()
            .append(modeName)
            .append(" has been ")
            .append(
                enabled
                    ? TextComponent.of("enabled", TextColor.GREEN)
                    : TextComponent.of("disabled", TextColor.RED))
            .color(TextColor.GRAY)
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
    TextComponent.Builder builder = TextComponent.builder().append("Slowmode has been ");

    if (isSlowmode()) {
      builder
          .append("enabled", TextColor.GREEN)
          .append(" in order to reduce spam.")
          .append(TextComponent.newline())
          .append(BroadcastUtils.BROADCAST_DIV.color(TextColor.GOLD))
          .append("Current slowmode speed: ")
          .append(TextComponent.of(getChatConfig().getSlowmodeSpeed(), TextColor.YELLOW))
          .append(formatSeconds(getChatConfig().getSlowmodeSpeed()));
    } else {
      builder
          .append("disabled", TextColor.RED)
          .append(". There is no longer a chat speed restriction in place");
    }
    return builder.color(TextColor.GRAY).build();
  }

  private Component getLockdownHover() {
    return TextComponent.of(
        isLockdown()
            ? "Chat messages can not be sent at this time"
            : "Chat messages can now be sent",
        TextColor.GRAY);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerChat(AsyncPlayerChatEvent event) {
    boolean exempt = event.getPlayer().hasPermission(CommunityPermissions.CHAT_MANAGEMENT);
    if (exempt) return;
    if (event.isAsynchronous()) return;

    Player sender = event.getPlayer();
    Audience viewer = Audience.get(sender);

    // Lockdown - Cancel ALL player chat, except staff
    if (isLockdown()) {
      viewer.sendWarning(TextComponent.of("The chat is currently locked"));
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
              TextComponent.builder()
                  .append("Please wait ")
                  .append(TextComponent.of(seconds, TextColor.RED, TextDecoration.BOLD))
                  .append(formatSeconds(seconds))
                  .append(" before sending another message")
                  .color(TextColor.GRAY)
                  .build();

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
          TextComponent.of("The chat has been locked.\nNo messages can be sent at this time"));
    } else if (isSlowmode()) {
      sendDelayedMessage(
          viewer,
          TextComponent.builder()
              .append("Chat slowmode has been ")
              .append("enabled", TextColor.GREEN)
              .append(" to reduce spam.")
              .append(TextComponent.newline())
              .append("You may send a chat message once every ")
              .append(TextComponent.of(getChatConfig().getSlowmodeSpeed(), TextColor.YELLOW))
              .append(formatSeconds(getChatConfig().getSlowmodeSpeed()))
              .color(TextColor.GRAY)
              .build());
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

  private String formatSeconds(long seconds) {
    return String.format(" second%s", seconds != 1 ? "s" : "");
  }
}

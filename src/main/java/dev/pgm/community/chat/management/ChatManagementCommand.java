package dev.pgm.community.chat.management;

import static net.kyori.adventure.text.Component.text;

import cloud.commandframework.annotations.CommandDescription;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import dev.pgm.community.Community;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.utils.BroadcastUtils;
import dev.pgm.community.utils.CommandAudience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import tc.oc.pgm.util.text.TextFormatter;

public class ChatManagementCommand extends CommunityCommand {

  private static final String CMD_NAME = "chat";

  private final ChatManagementFeature chat;

  public ChatManagementCommand() {
    this.chat = Community.get().getFeatures().getChatManagement();
  }

  @CommandMethod(CMD_NAME)
  @CommandDescription("View the current chat mode status")
  @CommandPermission(CommunityPermissions.CHAT_MANAGEMENT)
  public void viewStatus(CommandAudience audience) {
    audience.sendMessage(
        TextFormatter.horizontalLineHeading(
            audience.getSender(), text("Chat Status", NamedTextColor.YELLOW), NamedTextColor.GRAY));
    audience.sendMessage(formatStatus(text("Chat Lockdown"), chat.isLockdown()));
    audience.sendMessage(formatStatus(text("Chat Slowmode"), chat.isSlowmode()));
  }

  @CommandMethod(CMD_NAME + " lock")
  @CommandDescription("Toggle lock status for the chat")
  @CommandPermission(CommunityPermissions.CHAT_MANAGEMENT)
  public void toggleLock(CommandAudience viewer) {
    chat.toggleLockdown(viewer.getSender());
  }

  @CommandMethod(CMD_NAME + " slow")
  @CommandDescription("Toggle chat slowmode")
  @CommandPermission(CommunityPermissions.CHAT_MANAGEMENT)
  public void toggleSlowmode(CommandAudience viewer) {
    chat.toggleSlowmode(viewer.getSender());
  }

  @CommandMethod(CMD_NAME + " clear")
  @CommandDescription("Clear the global chat")
  @CommandPermission(CommunityPermissions.CHAT_MANAGEMENT)
  public void clearChat(CommandAudience viewer) {
    for (int i = 0; i < 100; i++) {
      BroadcastUtils.sendGlobalMessage(Component.empty());
    }
  }

  private Component formatStatus(Component name, boolean enabled) {
    return text()
        .append(BroadcastUtils.BROADCAST_DIV)
        .append(name.color(NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true))
        .append(text(": "))
        .append(
            enabled ? text("enabled", NamedTextColor.GREEN) : text("disabled", NamedTextColor.RED))
        .color(NamedTextColor.GRAY)
        .build();
  }
}

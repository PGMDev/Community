package dev.pgm.community.chat.management;

import static net.kyori.adventure.text.Component.text;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.utils.BroadcastUtils;
import dev.pgm.community.utils.CommandAudience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import tc.oc.pgm.util.text.TextFormatter;

@CommandAlias("chat")
@Description("Manage the chat status")
@CommandPermission(CommunityPermissions.CHAT_MANAGEMENT)
public class ChatManagementCommand extends CommunityCommand {

  @Dependency private ChatManagementFeature chat;

  @Subcommand("lock|lockdown")
  @Description("Toggle lock status for the chat")
  public void toggleLock(CommandAudience viewer) {
    chat.toggleLockdown(viewer.getSender());
  }

  @Subcommand("slow|slowmode")
  @Description("Toggle chat slowmode")
  public void toggleSlowmode(CommandAudience viewer) {
    chat.toggleSlowmode(viewer.getSender());
  }

  @Default
  @Subcommand("status")
  @Description("View the current chat mode status")
  public void viewStatus(CommandAudience audience) {
    audience.sendMessage(
        TextFormatter.horizontalLineHeading(
            audience.getSender(), text("Chat Status", NamedTextColor.YELLOW), NamedTextColor.GRAY));
    audience.sendMessage(formatStatus(text("Chat Lockdown"), chat.isLockdown()));
    audience.sendMessage(formatStatus(text("Chat Slowmode"), chat.isSlowmode()));
  }

  @Subcommand("clear")
  @CommandAlias("clearchat")
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

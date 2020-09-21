package dev.pgm.community.chat;

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
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextDecoration;
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
            audience.getSender(),
            TextComponent.of("Chat Status", TextColor.YELLOW),
            TextColor.GRAY));
    audience.sendMessage(formatStatus(TextComponent.of("Chat Lockdown"), chat.isLockdown()));
    audience.sendMessage(formatStatus(TextComponent.of("Chat Slowmode"), chat.isSlowmode()));
  }

  @Subcommand("clear")
  @CommandAlias("clearchat")
  public void clearChat(CommandAudience viewer) {
    for (int i = 0; i < 100; i++) {
      BroadcastUtils.sendGlobalMessage(TextComponent.empty());
    }

    Component clearMsg =
        TextComponent.builder()
            .append(BroadcastUtils.RIGHT_DIV.color(TextColor.GOLD))
            .append(" The chat has been cleared ")
            .append(BroadcastUtils.LEFT_DIV.color(TextColor.GOLD))
            .color(TextColor.GREEN)
            .build();

    BroadcastUtils.sendGlobalMessage(
        TextFormatter.horizontalLineHeading(viewer.getSender(), clearMsg, TextColor.BLACK));
  }

  private Component formatStatus(Component name, boolean enabled) {
    return TextComponent.builder()
        .append(BroadcastUtils.BROADCAST_DIV)
        .append(name.color(TextColor.GOLD).decoration(TextDecoration.BOLD, true))
        .append(": ")
        .append(
            enabled
                ? TextComponent.of("enabled", TextColor.GREEN)
                : TextComponent.of("disabled", TextColor.RED))
        .color(TextColor.GRAY)
        .build();
  }
}

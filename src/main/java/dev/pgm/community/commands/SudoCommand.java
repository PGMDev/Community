package dev.pgm.community.commands;

import static net.kyori.adventure.text.Component.text;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Syntax;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.utils.CommandAudience;
import net.kyori.adventure.text.format.NamedTextColor;

public class SudoCommand extends CommunityCommand {

  @CommandAlias("sudo|force")
  @Syntax(SELECTION + " <command>")
  @Description("Force targets to perform given command")
  @CommandCompletion("@players")
  @CommandPermission(CommunityPermissions.ADMIN)
  public void sudo(CommandAudience sender, String targets, String command) {
    if (sender.isPlayer() && !sender.getPlayer().isOp()) {
      sender.sendWarning(text("This command is reserved for administrators"));
      return;
    }

    PlayerSelection selection = getPlayers(sender, targets);
    if (!selection.getPlayers().isEmpty()) {
      final String targetCommand = command.startsWith("/") ? command.substring(1) : command;
      selection.getPlayers().forEach(player -> player.performCommand(targetCommand));
      sender.sendMessage(
          text()
              .append(text("Forcing "))
              .append(selection.getText())
              .append(text(" to run "))
              .append(text("/", NamedTextColor.AQUA))
              .append(text(targetCommand, NamedTextColor.AQUA))
              .color(NamedTextColor.GRAY)
              .build());
    } else {
      selection.sendNoPlayerComponent(sender);
    }
  }
}

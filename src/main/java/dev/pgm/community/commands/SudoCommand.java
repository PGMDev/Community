package dev.pgm.community.commands;

import static net.kyori.adventure.text.Component.text;

import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.utils.CommandAudience;
import net.kyori.adventure.text.format.NamedTextColor;
import tc.oc.pgm.lib.org.incendo.cloud.annotation.specifier.Greedy;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Argument;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Command;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.CommandDescription;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Permission;

public class SudoCommand extends CommunityCommand {

  @Command("sudo|force <targets> <command>")
  @CommandDescription("Force targets to perform given command")
  @Permission(CommunityPermissions.ADMIN)
  public void sudo(
      CommandAudience sender,
      @Argument("targets") String targets,
      @Argument("command") @Greedy String command) {
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

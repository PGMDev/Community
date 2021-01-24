package dev.pgm.community.poll.types;

import static net.kyori.adventure.text.Component.text;

import dev.pgm.community.poll.PollBase;
import dev.pgm.community.utils.BroadcastUtils;
import java.time.Duration;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;

/** ExecutablePoll - A poll which once completed executes a command */
public class ExecutablePoll extends PollBase {

  private String command;

  public ExecutablePoll(String command, Duration length, UUID playerId) {
    this(command, getDefaultText(command), length, false, playerId);
  }

  public ExecutablePoll(
      String command, String text, Duration length, boolean active, UUID playerId) {
    super(text, length, active, playerId);
    this.setCommand(command);
  }

  public String getCommand() {
    return command;
  }

  public void setCommand(String command) {
    this.command = command;
  }

  private static String getDefaultText(String command) {
    return "&6&lPoll&7: &eExecute &b/" + command;
  }

  @Override
  public void complete() {
    super.complete();
    if (getOutcome()) {
      if (getExecutor() != null) {
        Bukkit.dispatchCommand(getExecutor(), command);
      } else {
        BroadcastUtils.sendAdminChatMessage(
            text(
                "Poll could not be completed since original executor is offline",
                NamedTextColor.RED));
      }
    }

    Component yes =
        text()
            .append(text("Yes ("))
            .append(text(getVoteTally(true), NamedTextColor.GREEN))
            .append(text(")", NamedTextColor.GRAY))
            .build();
    Component no =
        text()
            .append(text(" - No ("))
            .append(text(getVoteTally(false), NamedTextColor.RED))
            .append(text(")", NamedTextColor.GRAY))
            .build();

    BroadcastUtils.sendGlobalMessage(
        text()
            .append(text("Poll: ", NamedTextColor.GOLD, TextDecoration.BOLD))
            .append(text("Results"))
            .append(BroadcastUtils.BROADCAST_DIV.color(NamedTextColor.GOLD))
            .append(yes)
            .append(no)
            .color(NamedTextColor.GRAY)
            .build());
  }
}

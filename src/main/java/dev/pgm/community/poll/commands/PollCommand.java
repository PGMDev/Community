package dev.pgm.community.poll.commands;

import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.poll.Poll;
import dev.pgm.community.poll.PollType;
import dev.pgm.community.poll.feature.PollFeature;
import dev.pgm.community.poll.types.ExecutablePoll;
import dev.pgm.community.utils.BroadcastUtils;
import dev.pgm.community.utils.CommandAudience;
import java.time.Duration;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent.Builder;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import tc.oc.pgm.util.bukkit.BukkitUtils;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.PlayerComponent;
import tc.oc.pgm.util.text.TemporalComponent;
import tc.oc.pgm.util.text.TextFormatter;

@CommandAlias("poll")
@Description("Create and manage polls")
@CommandPermission(CommunityPermissions.POLL)
public class PollCommand extends CommunityCommand {

  @Dependency private PollFeature polls;

  @Default
  @Description("View poll status")
  public void status(CommandAudience viewer) {
    if (polls.getPoll() != null) {
      Poll poll = polls.getPoll();
      viewer.sendMessage(
          TextFormatter.horizontalLineHeading(
              viewer.getSender(),
              text("Poll Info", NamedTextColor.DARK_AQUA),
              NamedTextColor.DARK_BLUE));
      viewer.sendMessage(text("Details:"));
      if (poll instanceof ExecutablePoll) {
        String currentCmd = ((ExecutablePoll) poll).getCommand();
        viewer.sendMessage(
            formatValue("Command")
                .append(text().append(text("/")).append(text(currentCmd)))
                .clickEvent(ClickEvent.suggestCommand("/poll cmd " + currentCmd))
                .build());
      }
      viewer.sendMessage(
          formatValue("Text")
              .append(text(poll.getText() != null ? BukkitUtils.colorize(poll.getText()) : ""))
              .clickEvent(ClickEvent.suggestCommand("/poll text " + poll.getText()))
              .build());
      viewer.sendMessage(
          formatValue("Executor")
              .append(
                  poll.getExecutor() == null
                      ? text("None")
                      : PlayerComponent.player(poll.getExecutor(), NameStyle.FANCY))
              .build());
      viewer.sendMessage(
          formatValue("Length")
              .append(TemporalComponent.duration(poll.getLength()).color(NamedTextColor.AQUA))
              .clickEvent(ClickEvent.suggestCommand("/poll time"))
              .build());
      viewer.sendMessage(text("Status:"));
      viewer.sendMessage(
          formatValue("Active")
              .append(
                  text(
                      poll.isActive() ? "Yes" : "No",
                      poll.isActive() ? NamedTextColor.GREEN : NamedTextColor.RED))
              .build());

      Component start = createActionButton("Start", "/poll start", NamedTextColor.GREEN);
      Component stop = createActionButton("Stop", "/poll stop", NamedTextColor.RED);
      if (poll.isActive()) {
        viewer.sendMessage(
            formatValue("Time Left")
                .append(TemporalComponent.duration(poll.getTimeLeft()))
                .build());
        viewer.sendMessage(text("Votes:"));
        viewer.sendMessage(
            formatValue("Yes").append(text(polls.getPoll().getVoteTally(true))).build());
        viewer.sendMessage(
            formatValue("No").append(text(polls.getPoll().getVoteTally(false))).build());
        viewer.sendMessage(space());
        viewer.sendMessage(stop);
      } else {
        viewer.sendMessage(space());
        viewer.sendMessage(start);
      }

    } else {
      viewer.sendWarning(text("There is no active poll. Create a new one with /poll create"));
    }
  }

  private Component createActionButton(String text, String command, NamedTextColor color) {
    return text()
        .append(text("[", NamedTextColor.DARK_GRAY))
        .append(text(text, color, TextDecoration.BOLD))
        .append(text("]", NamedTextColor.DARK_GRAY))
        .clickEvent(ClickEvent.runCommand(command))
        .hoverEvent(HoverEvent.showText(text("Click to start poll", NamedTextColor.GRAY)))
        .build();
  }

  private Builder formatValue(String valueName) {
    return text()
        .append(text(" - ", NamedTextColor.YELLOW))
        .append(text(valueName, NamedTextColor.GOLD))
        .append(space())
        .append(BroadcastUtils.RIGHT_DIV.color(NamedTextColor.YELLOW))
        .append(space());
  }

  @Subcommand("create|new|c")
  @Syntax("[command]")
  @Description("Create a new poll")
  public void create(CommandAudience audience, Player executor, String command) {
    if (polls.getPoll() == null) {
      polls.createPoll(
          PollType.COMMAND,
          command.startsWith("/") ? command.replaceFirst("/", "") : command,
          executor);
      BroadcastUtils.sendAdminChatMessage(
          text()
              .append(PlayerComponent.player(executor, NameStyle.FANCY))
              .append(
                  text(" has created a new ", NamedTextColor.GRAY)
                      .append(
                          text("Poll", NamedTextColor.GOLD, TextDecoration.BOLD)
                              .hoverEvent(
                                  HoverEvent.showText(
                                      text()
                                          .append(text("/"))
                                          .append(text(command))
                                          .color(NamedTextColor.YELLOW)))))
              .append(
                  text()
                      .append(text(" [", NamedTextColor.GRAY))
                      .append(text("Edit", NamedTextColor.DARK_PURPLE))
                      .append(text("]", NamedTextColor.GRAY))
                      .hoverEvent(
                          HoverEvent.showText(text("Click to edit poll", NamedTextColor.GRAY))))
              .append(
                  text()
                      .append(text(" [", NamedTextColor.GRAY))
                      .append(text("Start", NamedTextColor.GREEN))
                      .append(text("]", NamedTextColor.GRAY))
                      .clickEvent(ClickEvent.runCommand("/poll start"))
                      .hoverEvent(
                          HoverEvent.showText(text("Click to start poll", NamedTextColor.GRAY))))
              .clickEvent(ClickEvent.runCommand("/poll"))
              .build());
    } else {
      audience.sendWarning(text("There is already an existing poll"));
    }
  }

  @Subcommand("text|description|t")
  @Description("Set a custom prompt for poll")
  public void setText(CommandAudience audience, @Optional String text) {
    if (polls.getPoll() == null) {
      audience.sendWarning(text("There is no existing poll to edit"));
      return;
    }

    polls.getPoll().setText(text);

    BroadcastUtils.sendAdminChatMessage(
        text()
            .append(audience.getStyledName())
            .append(
                text == null
                    ? text(" cleared the poll text")
                    : text()
                        .append(text(" set the poll text to "))
                        .append(text(BukkitUtils.colorize(polls.getPoll().getText())))
                        .build())
            .color(NamedTextColor.GRAY)
            .build());
  }

  @Subcommand("length|duration|time|tl")
  @Description("Set the length of the poll")
  public void setLength(CommandAudience audience, Duration time) {
    if (polls.getPoll() == null) {
      audience.sendWarning(text("There is no existing poll to edit"));
      return;
    }

    polls.getPoll().setLength(time.abs());

    BroadcastUtils.sendAdminChatMessage(
        text()
            .append(audience.getStyledName())
            .append(
                text()
                    .append(text(" set poll length to "))
                    .append(TemporalComponent.duration(time).color(NamedTextColor.AQUA))
                    .build())
            .color(NamedTextColor.GRAY)
            .build());
  }

  @Subcommand("command|cmd")
  @Description("Set the poll command")
  public void setCommand(CommandAudience audience, Player executor, String command) {
    if (polls.getPoll() == null || !(polls.getPoll() instanceof ExecutablePoll)) {
      audience.sendWarning(text("There is no existing command poll to edit"));
      return;
    }

    ExecutablePoll poll = (ExecutablePoll) polls.getPoll();
    poll.setCommand(command);
    poll.setExecutor(executor.getUniqueId());

    BroadcastUtils.sendAdminChatMessage(
        text()
            .append(audience.getStyledName())
            .append(text(" set poll command to "))
            .append(
                text()
                    .append(text("/"))
                    .append(text(poll.getCommand()))
                    .color(NamedTextColor.YELLOW))
            .color(NamedTextColor.GRAY)
            .build());
  }

  @Subcommand("start|begin|s")
  @Description("Start the poll")
  public void startPoll(CommandAudience audience) {
    if (polls.getPoll() == null) {
      audience.sendWarning(text("There is no existing poll to start"));
      return;
    }

    if (!polls.start()) {
      audience.sendWarning(text("Poll has already started!"));
    }
  }

  @Subcommand("cancel|stop")
  @Description("Stops the poll and does not execute the completion")
  public void cancelPoll(CommandAudience audience) {
    if (polls.getPoll() == null) {
      audience.sendWarning(text("There is no poll to cancel"));
      return;
    }

    if (!polls.cancel()) {
      audience.sendWarning(text("No poll is running"));
    }
  }
}

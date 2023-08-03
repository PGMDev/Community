package dev.pgm.community.polls;

import static net.kyori.adventure.text.Component.text;

import com.google.common.collect.Lists;
import dev.pgm.community.polls.ending.EndAction;
import dev.pgm.community.polls.ending.types.NullEndAction;
import dev.pgm.community.polls.types.TimedPoll;
import dev.pgm.community.utils.BroadcastUtils;
import dev.pgm.community.utils.CenterUtils;
import dev.pgm.community.utils.CommandAudience;
import dev.pgm.community.utils.MessageUtils;
import dev.pgm.community.utils.Sounds;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import tc.oc.pgm.util.text.TemporalComponent;
import tc.oc.pgm.util.text.TextFormatter;

public interface PollComponents {

  default Component getYesButton() {
    return formatIconButton(
        MessageUtils.ACCEPT, "Yes", NamedTextColor.DARK_GREEN, "/yes", "Click to vote yes!");
  }

  default Component getNoButton() {
    return formatIconButton(
        MessageUtils.DENY, "No", NamedTextColor.RED, "/no", "Click to vote no!");
  }

  default Component getVoteButtons() {
    return text()
        .append(getYesButton())
        .appendSpace()
        .appendSpace()
        .appendSpace()
        .append(getNoButton())
        .build();
  }

  default Component getFooter() {
    return TextFormatter.horizontalLine(NamedTextColor.GRAY, TextFormatter.MAX_CHAT_WIDTH);
  }

  default Component formatCategoryDetail(String category, Component value) {
    return text()
        .append(text(category, NamedTextColor.GOLD, TextDecoration.BOLD))
        .append(text(": ", NamedTextColor.GRAY))
        .append(value)
        .build();
  }

  default void sendPollBroadcast(Poll poll) {
    List<Component> lines = Lists.newArrayList();
    lines.add(
        TextFormatter.horizontalLineHeading(
            null, text("Server Poll", NamedTextColor.AQUA), NamedTextColor.YELLOW));
    lines.add(text(" "));

    Component question =
        text()
            .append(text("Question", NamedTextColor.GOLD, TextDecoration.BOLD))
            .append(text(": ", NamedTextColor.GRAY))
            .append(poll.getQuestion())
            .build();

    lines.add(CenterUtils.centerComponent(question));
    lines.add(text(" "));
    lines.add(CenterUtils.centerComponent(getVoteButtons()));
    lines.add(text(" "));
    lines.add(TextFormatter.horizontalLine(NamedTextColor.YELLOW, TextFormatter.MAX_CHAT_WIDTH));

    BroadcastUtils.sendMultiLineGlobal(lines, Sounds.ALERT);
  }

  default void sendPollResults(Poll poll, long yay, long nay, boolean success) {
    List<Component> lines = Lists.newArrayList();

    final NamedTextColor lineColor = success ? NamedTextColor.GREEN : NamedTextColor.RED;

    Component header =
        TextFormatter.horizontalLineHeading(
            null, text("Poll Results", NamedTextColor.AQUA), lineColor);
    Component footer = TextFormatter.horizontalLine(lineColor, TextFormatter.MAX_CHAT_WIDTH);

    Component question =
        text()
            .append(text("Question", NamedTextColor.GOLD, TextDecoration.BOLD))
            .append(text(": ", NamedTextColor.GRAY))
            .append(poll.getQuestion())
            .build();

    Component finalResult =
        text()
            .append(text("Final Result", NamedTextColor.GOLD, TextDecoration.BOLD))
            .append(text(": ", NamedTextColor.GRAY))
            .append(text(success ? "YES" : "NO", lineColor, TextDecoration.UNDERLINED))
            .hoverEvent(HoverEvent.showText(createYesNoInfo(yay, nay)))
            .build();

    Component graphBreakdown = createGraphBreakdown(yay, nay);

    lines.add(header);
    lines.add(text(" "));
    lines.add(CenterUtils.centerComponent(question));
    lines.add(text(" "));
    lines.add(CenterUtils.centerComponent(finalResult));
    lines.add(CenterUtils.centerComponent(graphBreakdown));
    lines.add(footer);

    BroadcastUtils.sendMultiLineGlobal(lines, Sounds.ALERT);
  }

  default Component createGraphBreakdown(long yay, long nay) {
    final int maxBarLength = 17; // Max length of the bar in characters
    final long totalVotes = yay + nay;
    final double yayPercentage = (double) yay / totalVotes;
    final double nayPercentage = (double) nay / totalVotes;

    int yayBarLength = (int) (yayPercentage * maxBarLength);
    int nayBarLength = (int) (nayPercentage * maxBarLength);

    Component graphBar = createColoredBar(yayBarLength, nayBarLength, maxBarLength);

    return text()
        .append(text("Votes: ", NamedTextColor.GRAY))
        .append(graphBar)
        .appendSpace()
        .append(createYesNoInfo(yay, nay))
        .build();
  }

  default Component createColoredBar(int yayBarLength, int nayBarLength, int maxBarLength) {
    TextComponent.Builder builder = text();

    final Component SQUARE = text("\u2b1b"); // Filled square
    // final Component SQUARE = text("\u2b1c"); - Hollow square

    for (int i = 0; i < maxBarLength; i++) {
      if (i < yayBarLength) {
        builder.append(SQUARE.color(NamedTextColor.DARK_GREEN));
      } else if (i < yayBarLength + nayBarLength) {
        builder.append(SQUARE.color(NamedTextColor.DARK_RED));
      }
    }

    return builder.build();
  }

  default Component createYesNoInfo(long yay, long nay) {
    return text()
        .append(text("(", NamedTextColor.DARK_GRAY))
        .append(text(yay, NamedTextColor.DARK_GREEN))
        .append(text(" YES", NamedTextColor.GREEN))
        .append(text(" | ", NamedTextColor.DARK_GRAY))
        .append(text(nay, NamedTextColor.DARK_RED))
        .append(text(" NO", NamedTextColor.RED))
        .append(text(")", NamedTextColor.DARK_GRAY))
        .build();
  }

  default Component formatIconButton(
      Component icon, String text, NamedTextColor color, String command, String hover) {
    return text()
        .append(text("[", NamedTextColor.GRAY))
        .append(icon)
        .appendSpace()
        .append(text(text, color))
        .append(text("]", NamedTextColor.GRAY))
        .appendSpace()
        .hoverEvent(HoverEvent.showText(text(hover, NamedTextColor.GRAY)))
        .clickEvent(ClickEvent.runCommand(command))
        .build();
  }

  default Component formatButton(
      String text, NamedTextColor color, String command, String hover, boolean suggest) {
    return text()
        .append(text("[", NamedTextColor.GRAY))
        .append(text(text, color))
        .append(text("]", NamedTextColor.GRAY))
        .appendSpace()
        .hoverEvent(HoverEvent.showText(text(hover, NamedTextColor.GRAY)))
        .clickEvent(suggest ? ClickEvent.suggestCommand(command) : ClickEvent.runCommand(command))
        .build();
  }

  default Component createVoteReminderBroadcast(Poll poll, Duration timeLeft, boolean hasVoted) {
    TextComponent.Builder alert =
        text()
            .append(text("[", NamedTextColor.GRAY))
            .append(text("Poll", NamedTextColor.AQUA))
            .append(text("]", NamedTextColor.GRAY))
            .appendSpace()
            .append(text("Only "))
            .append(TemporalComponent.duration(timeLeft, urgencyColor(timeLeft)))
            .append(text(" left to vote! "))
            .color(NamedTextColor.DARK_AQUA)
            .hoverEvent(HoverEvent.showText(poll.getQuestion()));

    if (!hasVoted) {
      alert.append(getYesButton()).appendSpace().append(getNoButton());
    }

    return alert.build();
  }

  // Yoinked out of MatchCountdown
  default TextColor urgencyColor(Duration remaining) {
    long seconds = remaining.getSeconds();
    if (seconds > 60) {
      return NamedTextColor.GREEN;
    } else if (seconds > 30) {
      return NamedTextColor.YELLOW;
    } else if (seconds > 5) {
      return NamedTextColor.GOLD;
    } else {
      return NamedTextColor.DARK_RED;
    }
  }

  default void sendPollDetails(Poll poll, CommandAudience audience) {
    if (poll == null) {
      audience.sendWarning(text("No active poll found!"));
      return;
    }

    // Calc duration
    Duration duration =
        poll.getEndTime() != null ? Duration.between(poll.getStartTime(), poll.getEndTime()) : null;

    sendDetails(
        audience, "Poll Details", poll.getQuestion(), duration, poll.getEndAction(), false, false);

    if (poll instanceof TimedPoll) {
      TimedPoll timedPoll = (TimedPoll) poll;
      audience.sendMessage(
          formatCategoryDetail("Time Left", TemporalComponent.clock(timedPoll.getTimeLeft())));
    } else {
      Duration timeSinceStart = Duration.between(poll.getStartTime(), Instant.now());
      audience.sendMessage(
          formatCategoryDetail(
              "Time Since Start", TemporalComponent.duration(timeSinceStart, NamedTextColor.AQUA)));
    }
    audience.sendMessage(formatCategoryDetail("Total Votes", text(poll.getVotes().size())));
    audience.sendMessage(
        formatButton("End", NamedTextColor.RED, "/poll end", "Click to end the poll", false));
    audience.sendMessage(getFooter());
  }

  default void sendBuilderDetails(PollBuilder builder, CommandAudience audience) {
    sendDetails(
        audience,
        "Poll Setup Details",
        builder.getQuestion(),
        builder.getDuration(),
        builder.getEndAction(),
        true,
        !builder.canBuild());

    if (builder.canBuild()) {
      Component buttons =
          text()
              .append(
                  formatButton(
                      "Start", NamedTextColor.GREEN, "/poll start", "Click to start poll", false))
              .appendSpace()
              .append(
                  formatButton(
                      "Reset", NamedTextColor.RED, "/poll reset", "Click to reset values", false))
              .build();

      audience.sendMessage(buttons);
      audience.sendMessage(getFooter());
    }
  }

  default void sendDetails(
      CommandAudience audience,
      String title,
      Component question,
      Duration duration,
      EndAction action,
      boolean builder,
      boolean footer) {

    audience.sendMessage(
        TextFormatter.horizontalLineHeading(
            audience.getSender(), text(title, NamedTextColor.AQUA), NamedTextColor.GRAY));

    Component endAction =
        text()
            .append(action.getName().color(NamedTextColor.DARK_GREEN))
            .append(
                action.getPreviewValue() != null
                    ? text().append(BroadcastUtils.BROADCAST_DIV).append(action.getPreviewValue())
                    : text(""))
            .build();

    if (builder && action instanceof NullEndAction) {
      endAction =
          endAction
              .append(text(" - ", NamedTextColor.GRAY))
              .append(
                  formatButton(
                      "Command",
                      NamedTextColor.YELLOW,
                      "/poll command",
                      "Click to set a command",
                      true))
              .append(
                  formatButton("Map", NamedTextColor.GOLD, "/poll map", "Click to set a map", true))
              .append(
                  formatButton(
                      "Kick", NamedTextColor.RED, "/poll kick", "Click to kick a player", true));
    }

    Component durationComponent =
        duration == null
            ? text()
                .append(text("Open Ended", NamedTextColor.DARK_PURPLE))
                .hoverEvent(
                    HoverEvent.showText(
                        text()
                            .append(text("No time limit. Poll will end when "))
                            .append(text("/poll end", NamedTextColor.AQUA))
                            .append(text(" is executed."))
                            .color(NamedTextColor.GRAY)
                            .build()))
                .build()
            : TemporalComponent.duration(duration);

    if (builder && duration == null) {
      durationComponent =
          durationComponent
              .append(text(" - ", NamedTextColor.GRAY))
              .append(
                  formatButton(
                      "1m",
                      NamedTextColor.DARK_AQUA,
                      "/poll duration 1m",
                      "Click to set a 1 minute time limit",
                      false))
              .append(
                  formatButton(
                      "5m",
                      NamedTextColor.DARK_AQUA,
                      "/poll duration 5m",
                      "Click to set a 5 minute time limit",
                      false))
              .append(
                  formatButton(
                      "Custom",
                      NamedTextColor.DARK_AQUA,
                      "/poll duration",
                      "Click to set a custom time limit",
                      true));
    }

    if (question == null) {
      question = action.getDefaultQuestion();
    }

    audience.sendMessage(formatCategoryDetail("Question", question));
    audience.sendMessage(formatCategoryDetail("Duration", durationComponent));
    audience.sendMessage(formatCategoryDetail("End Action", endAction));

    audience.sendMessage(text(""));

    if (footer) {
      audience.sendMessage(getFooter());
    }
  }
}

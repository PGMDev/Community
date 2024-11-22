package dev.pgm.community.polls;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;

import com.google.common.collect.Lists;
import dev.pgm.community.polls.commands.PollVoteCommands;
import dev.pgm.community.polls.ending.EndAction;
import dev.pgm.community.polls.ending.types.NullEndAction;
import dev.pgm.community.utils.BroadcastUtils;
import dev.pgm.community.utils.CenterUtils;
import dev.pgm.community.utils.CommandAudience;
import dev.pgm.community.utils.MessageUtils;
import dev.pgm.community.utils.Sounds;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
        MessageUtils.ACCEPT,
        "Yes",
        NamedTextColor.DARK_GREEN,
        "/" + PollVoteCommands.COMMAND + " yes",
        "Click to vote yes!");
  }

  default Component getNoButton() {
    return formatIconButton(
        MessageUtils.DENY,
        "No",
        NamedTextColor.RED,
        "/" + PollVoteCommands.COMMAND + " no",
        "Click to vote no!");
  }

  default Component getBooleanVoteButtons(boolean compact) {
    TextComponent.Builder buttons = text().append(getYesButton()).appendSpace();

    if (!compact) {
      buttons.appendSpace().appendSpace();
    }

    buttons.append(getNoButton());

    return buttons.build();
  }

  default Component generateVoteButtons(List<EndAction> options, boolean mixed) {
    TextComponent.Builder builder = text();
    for (int i = 0; i < options.size(); i++) {
      EndAction option = options.get(i);
      Component optionBtn = formatEndActionButton(option, i, mixed);
      builder.append(optionBtn).appendSpace();
    }
    return builder.build();
  }

  default Component formatEndActionButton(EndAction action, int index, boolean mixed) {
    TextComponent.Builder hover = text();

    HoverEvent<?> existingHover = action.getButtonValue(mixed).hoverEvent();
    if (existingHover != null) {
      TextComponent existingHoverMsg = (TextComponent) existingHover.value();
      hover.append(existingHoverMsg).appendNewline();
    }

    hover
        .append(text("Click to vote for ", NamedTextColor.GRAY))
        .append(action.getButtonValue(mixed));

    return text()
        .append(text("[", NamedTextColor.GRAY))
        .append(action.getButtonValue(mixed).hoverEvent(null))
        .append(text("]", NamedTextColor.GRAY))
        .hoverEvent(HoverEvent.showText(hover.build()))
        .clickEvent(ClickEvent.runCommand("/" + PollVoteCommands.COMMAND + " " + index))
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

  default Component formatQuestion(Poll poll) {
    Component question =
        text()
            .append(text("Question", NamedTextColor.GOLD, TextDecoration.BOLD))
            .append(text(": ", NamedTextColor.GRAY))
            .hoverEvent(
                HoverEvent.showText(
                    text()
                        .append(poll.getRequiredThreshold().toComponent())
                        .append(text(" required for success.", NamedTextColor.GRAY))))
            .build();

    return text().append(question).append(poll.getQuestion()).build();
  }

  default void sendPollBroadcast(Poll poll) {
    List<Component> lines = Lists.newArrayList();
    lines.add(
        TextFormatter.horizontalLineHeading(
            null, text("Server Poll", NamedTextColor.AQUA), NamedTextColor.YELLOW));
    lines.add(text(" "));

    Component question = formatQuestion(poll);

    lines.add(CenterUtils.centerComponent(question));
    lines.add(text(" "));
    lines.add(CenterUtils.centerComponent(poll.getVoteButtons(false)));
    lines.add(text(" "));
    lines.add(TextFormatter.horizontalLine(NamedTextColor.YELLOW, TextFormatter.MAX_CHAT_WIDTH));

    BroadcastUtils.sendMultiLineGlobal(lines, Sounds.ALERT);
  }

  default void sendBooleanPollResults(Poll poll, long yay, long nay, boolean success) {
    List<Component> lines = Lists.newArrayList();

    final NamedTextColor lineColor = success ? NamedTextColor.GREEN : NamedTextColor.RED;

    Component header =
        TextFormatter.horizontalLineHeading(
            null, text("Poll Results", NamedTextColor.AQUA), lineColor);
    Component footer = TextFormatter.horizontalLine(lineColor, TextFormatter.MAX_CHAT_WIDTH);

    Component question = formatQuestion(poll);

    Component finalResult =
        text()
            .append(text("Final Result", NamedTextColor.GOLD, TextDecoration.BOLD))
            .append(text(": ", NamedTextColor.GRAY))
            .append(text(success ? "YES" : "NO", lineColor, TextDecoration.UNDERLINED))
            .hoverEvent(
                HoverEvent.showText(
                    text()
                        .append(poll.getRequiredThreshold().toComponent())
                        .appendNewline()
                        .append(createYesNoInfo(yay, nay))))
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

  default void sendMultiChoicePollResults(
      Poll poll, Map<EndAction, Integer> voteCounts, EndAction winningAction) {
    List<Component> lines = Lists.newArrayList();

    Component header =
        TextFormatter.horizontalLineHeading(
            null, text("Poll Results", NamedTextColor.AQUA), NamedTextColor.GREEN);
    Component footer =
        TextFormatter.horizontalLine(NamedTextColor.GREEN, TextFormatter.MAX_CHAT_WIDTH);

    Component question = formatQuestion(poll);

    Component finalResult =
        text()
            .append(text("Final Result", NamedTextColor.GOLD, TextDecoration.BOLD))
            .append(text(": ", NamedTextColor.GRAY))
            .append(winningAction.getPreviewValue().decorate(TextDecoration.UNDERLINED))
            .hoverEvent(
                HoverEvent.showText(
                    text()
                        .append(poll.getRequiredThreshold().toComponent())
                        .appendNewline()
                        .append(createMultiChoiceInfo(voteCounts))))
            .build();

    Component graphBreakdown = createRankedGraphBreakdown(voteCounts, winningAction);

    lines.add(header);
    lines.add(text(" "));
    lines.add(CenterUtils.centerComponent(question));
    lines.add(text(" "));
    lines.add(CenterUtils.centerComponent(finalResult));
    lines.add(CenterUtils.centerComponent(graphBreakdown));
    lines.add(footer);

    BroadcastUtils.sendMultiLineGlobal(lines, Sounds.ALERT);
  }

  default Component createMultiChoiceInfo(Map<EndAction, Integer> voteCounts) {
    TextComponent.Builder builder = text().append(text("Votes: ", NamedTextColor.GRAY));

    boolean first = true;
    for (Map.Entry<EndAction, Integer> entry : voteCounts.entrySet()) {
      if (!first) {
        builder.append(text(" | ", NamedTextColor.DARK_GRAY));
      } else {
        first = false;
      }

      builder
          .append(text("(", NamedTextColor.DARK_GRAY))
          .append(text(entry.getValue(), NamedTextColor.GRAY))
          .append(text(" "))
          .append(entry.getKey().getPreviewValue())
          .append(text(")", NamedTextColor.DARK_GRAY));
    }

    return builder.build();
  }

  default Component createRankedGraphBreakdown(
      Map<EndAction, Integer> voteCounts, EndAction winningAction) {
    List<EndAction> rankedActions =
        voteCounts.entrySet().stream()
            .sorted(Map.Entry.<EndAction, Integer>comparingByValue().reversed())
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

    final int maxBarLength = 17; // Max length of the bar in characters
    int totalVotes = rankedActions.stream().mapToInt(voteCounts::get).sum();

    TextComponent.Builder builder = text().append(text("Votes: ", NamedTextColor.GRAY));

    for (EndAction action : rankedActions) {
      int actionVotes = voteCounts.getOrDefault(action, 0);
      double actionPercentage = (double) actionVotes / totalVotes;
      int actionBarLength = (int) (actionPercentage * maxBarLength);

      NamedTextColor barColor;
      if (action == winningAction) {
        barColor = NamedTextColor.DARK_GREEN;
      } else if (rankedActions.indexOf(action) == 1) {
        barColor = NamedTextColor.GOLD;
      } else if (rankedActions.indexOf(action) == 2) {
        barColor = NamedTextColor.YELLOW;
      } else if (rankedActions.indexOf(action) == 3) {
        barColor = NamedTextColor.RED;
      } else {
        barColor = NamedTextColor.GRAY;
      }

      builder.append(
          createHoverableBar(
              action, actionBarLength, maxBarLength, barColor, actionVotes, actionPercentage));
    }

    builder
        .append(text(" (", NamedTextColor.GRAY))
        .append(text(totalVotes, NamedTextColor.DARK_GREEN))
        .append(text(" total vote" + (totalVotes != 1 ? "s" : ""), NamedTextColor.GRAY))
        .append(text(")", NamedTextColor.GRAY));

    return builder.build();
  }

  default Component createHoverableBar(
      EndAction action,
      int barLength,
      int maxBarLength,
      NamedTextColor barColor,
      int actionVotes,
      double actionPercentage) {
    TextComponent.Builder builder = text();

    final Component SQUARE = text("\u2b1b", barColor); // Filled square

    for (int i = 0; i < maxBarLength; i++) {
      if (i < barLength) {
        Component hoverText =
            text()
                .append(action.getPreviewValue().color(barColor))
                .append(text(": ", NamedTextColor.GRAY))
                .append(text(actionVotes + " vote" + (actionVotes != 1 ? "s" : ""), barColor))
                .append(text(" (", NamedTextColor.GRAY))
                .append(text(String.format("%.2f", actionPercentage * 100) + "%", barColor))
                .append(text(")", NamedTextColor.GRAY))
                .build();
        builder.append(SQUARE.hoverEvent(HoverEvent.showText(hoverText)));
      }
    }

    return builder.build();
  }

  default Component formatIconButton(
      Component icon, String text, NamedTextColor color, String command, String hover) {
    Component textComponent = empty();

    if (text != null) {
      textComponent = text(" " + text, color);
    }

    return text()
        .append(text("[", NamedTextColor.GRAY))
        .append(icon)
        .append(textComponent)
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
      alert.append(poll.getVoteButtons(true));
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
        audience,
        "Poll Details",
        poll.getQuestion(),
        duration,
        poll.getRequiredThreshold(),
        poll.getEndAction(),
        false,
        false);

    audience.sendMessage(
        formatCategoryDetail("Time Left", TemporalComponent.clock(poll.getTimeLeft())));
    audience.sendMessage(formatCategoryDetail("Total Votes", text(poll.getTotalVotes())));
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
        builder.getThreshold(),
        builder.getEndAction().stream().collect(Collectors.toList()),
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
      PollThreshold threshold,
      List<EndAction> options,
      boolean builder,
      boolean footer) {

    audience.sendMessage(
        TextFormatter.horizontalLineHeading(
            audience.getSender(), text(title, NamedTextColor.AQUA), NamedTextColor.GRAY));

    List<Component> optionLines = Lists.newArrayList();
    int i = 0;
    for (EndAction e : options) {
      optionLines.add(createEndActionLine(e, i, options.size() > 1 && builder));
      i++;
    }

    if (builder && optionLines.isEmpty()) {
      optionLines.add(
          createEndActionLine(new NullEndAction(), 0, false)
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
                      "Kick", NamedTextColor.RED, "/poll kick", "Click to kick a player", true)));
    }

    Component durationComponent = TemporalComponent.duration(duration, NamedTextColor.GREEN);

    if (builder) {
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

    Component thresholdComponent =
        text()
            .append(threshold.toComponent())
            .append(text(" for vote to pass.", NamedTextColor.GRAY))
            .hoverEvent(HoverEvent.showText(text("Click to adjust threshold", NamedTextColor.GRAY)))
            .clickEvent(ClickEvent.suggestCommand("/poll threshold"))
            .build();

    if (question == null) {
      question = PollBuilder.generateQuestion(options);
    }

    audience.sendMessage(formatCategoryDetail("Question", question));
    audience.sendMessage(formatCategoryDetail("Duration", durationComponent));
    audience.sendMessage(formatCategoryDetail("Threshold", thresholdComponent));
    audience.sendMessage(
        formatCategoryDetail(options.size() > 1 ? "Options" : "End Action", empty()));

    for (Component line : optionLines) {
      audience.sendMessage(line);
    }

    audience.sendMessage(text(""));

    if (footer) {
      audience.sendMessage(getFooter());
    }
  }

  default Component createEndActionLine(EndAction action, int index, boolean showRemove) {
    Component remove =
        formatIconButton(
            MessageUtils.DENY,
            null,
            NamedTextColor.RED,
            "/poll remove " + action.getValue(),
            "Click to remove");

    TextComponent.Builder line = text();

    if (showRemove) {
      line.append(BroadcastUtils.RIGHT_DIV).appendSpace();
    }

    line.append(action.getName().color(NamedTextColor.DARK_GREEN))
        .append(
            action.getPreviewValue() != null
                ? text().append(BroadcastUtils.BROADCAST_DIV).append(action.getPreviewValue())
                : text(""));

    if (showRemove) {
      line.appendSpace().append(remove);
    }

    return line.build();
  }
}

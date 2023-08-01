package dev.pgm.community.polls.feature;

import static net.kyori.adventure.text.Component.text;

import com.google.common.collect.Lists;
import dev.pgm.community.Community;
import dev.pgm.community.feature.FeatureBase;
import dev.pgm.community.polls.Poll;
import dev.pgm.community.polls.PollBuilder;
import dev.pgm.community.polls.PollConfig;
import dev.pgm.community.polls.PollEditAlerter;
import dev.pgm.community.polls.ending.EndAction;
import dev.pgm.community.polls.types.TimedPoll;
import dev.pgm.community.utils.BroadcastUtils;
import dev.pgm.community.utils.CenterUtils;
import dev.pgm.community.utils.CommandAudience;
import dev.pgm.community.utils.MessageUtils;
import dev.pgm.community.utils.Sounds;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.text.TemporalComponent;
import tc.oc.pgm.util.text.TextFormatter;

public class PollFeature extends FeatureBase implements PollEditAlerter {

  private PollBuilder builder;
  private Poll poll;

  private boolean delayedStart;
  private Integer delayedTaskID;

  public PollFeature(Configuration config, Logger logger) {
    super(new PollConfig(config), logger, "Polls");

    if (getConfig().isEnabled()) {
      enable();
      Bukkit.getScheduler().scheduleSyncRepeatingTask(Community.get(), this::task, 0L, 20L);
    }
  }

  private void task() {
    if (!isRunning()) return;

    if (poll instanceof TimedPoll) {
      TimedPoll timedPoll = (TimedPoll) poll;
      Duration timeLeft = timedPoll.getTimeLeft();

      if (shouldShowAlert(timeLeft)) {
        Component alert =
            text()
                .append(text("[", NamedTextColor.GRAY))
                .append(text("Poll", NamedTextColor.AQUA))
                .append(text("]", NamedTextColor.GRAY))
                .appendSpace()
                .append(text("Only "))
                .append(TemporalComponent.duration(timeLeft, urgencyColor(timeLeft)))
                .append(text(" left to vote! "))
                .color(NamedTextColor.DARK_AQUA)
                .hoverEvent(HoverEvent.showText(poll.getQuestion()))
                .build();

        for (Player player :
            Bukkit.getOnlinePlayers()) { // Send to each player so we can show/hide vote buttons
          Audience viewer = Audience.get(player);
          if (!timedPoll.hasVoted(player)) {
            viewer.sendMessage(alert.append(YES_BUTTON).appendSpace().append(NO_BUTTON));
          } else {
            viewer.sendMessage(alert);
          }
        }
      }

      if (timeLeft.getSeconds() == 0) {
        end(null);
      }
    }
  }

  public boolean isRunning() {
    return poll != null && poll.isRunning();
  }

  public Poll getPoll() {
    return poll;
  }

  public PollBuilder getBuilder() {
    if (builder == null) {
      resetBuilder();
    }
    return builder;
  }

  public void resetBuilder() {
    this.builder = new PollBuilder(this);
  }

  public boolean canStart() {
    return getBuilder().canBuild();
  }

  public boolean isDelayedStartScheduled() {
    return delayedStart;
  }

  public void start(CommandAudience audience) {
    if (builder == null) return; // No poll created yet

    // Old poll, remove first
    if (isRunning()) {
      audience.sendWarning(text("The poll has already started!"));
      return;
    }

    // Reset delayed start
    delayedStart = false;
    delayedTaskID = null;

    // Let staff know who started the poll
    if (audience != null) {
      broadcastChange(audience, "Started Poll");
    }

    // Build the poll
    poll = builder.build();

    // Broadcast Poll info
    sendPollBroadcast();
  }

  public void delayedStart(CommandAudience audience, Duration delay) {

    // Let staff know who started the poll
    broadcastChange(audience, "Delayed Poll Start", delay);

    delayedStart = true;

    // Delayed start
    delayedTaskID =
        Bukkit.getScheduler()
            .scheduleSyncDelayedTask(Community.get(), () -> start(null), 20L * delay.getSeconds());
  }

  public void end(CommandAudience audience) {

    if (delayedStart && delayedTaskID != null) {
      delayedStart = false;
      Bukkit.getScheduler().cancelTask(delayedTaskID);
      broadcastChange(audience, "Cancelled Delayed Poll Start");
      return;
    }

    if (poll == null) return; // No poll created yet

    broadcastChange(audience, "Ended Poll");

    if (poll != null && poll.getEndTime() == null) {
      poll.setEndTime(Instant.now());
    }

    long yesVotes = poll.getVotes().values().stream().filter(Boolean::booleanValue).count();
    long noVotes = poll.getVotes().size() - yesVotes;

    boolean majorityOption = yesVotes > noVotes;

    sendPollResults(yesVotes, noVotes, majorityOption);

    if (majorityOption) {
      Player creatorPlayer = Bukkit.getPlayer(poll.getCreator());
      poll.getEndAction().execute(creatorPlayer);
    }

    poll = null; // Reset the poll
    resetBuilder(); // Reset builder so values are clean
  }

  public void vote(CommandAudience viewer, Player sender, boolean vote) {
    if (poll == null) {
      viewer.sendWarning(text("There's no poll to vote for at this time!"));
      return;
    }

    if (!poll.vote(sender, vote)) {
      viewer.sendWarning(text("You have already voted for this poll!"));
      return;
    }

    viewer.sendMessage(
        text("Thanks for voting! The results will be announced soon.", NamedTextColor.GREEN));
  }

  public void sendPollResults(long yay, long nay, boolean success) {
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

  private Component createGraphBreakdown(long yay, long nay) {
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

  private Component createColoredBar(int yayBarLength, int nayBarLength, int maxBarLength) {
    TextComponent.Builder builder = text();

    // final Component SQUARE = text("\u2b1b");
    final Component SQUARE = text("\u2b1c");

    for (int i = 0; i < maxBarLength; i++) {
      if (i < yayBarLength) {
        builder.append(SQUARE.color(NamedTextColor.DARK_GREEN));
      } else if (i < yayBarLength + nayBarLength) {
        builder.append(SQUARE.color(NamedTextColor.DARK_RED));
      }
    }

    return builder.build();
  }

  private Component createYesNoInfo(long yay, long nay) {
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

  public void sendPollBroadcast() {
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
    lines.add(CenterUtils.centerComponent(VOTE_BUTTONS));
    lines.add(text(" "));
    lines.add(TextFormatter.horizontalLine(NamedTextColor.YELLOW, TextFormatter.MAX_CHAT_WIDTH));

    BroadcastUtils.sendMultiLineGlobal(lines, Sounds.ALERT);
  }

  private static final Component YES_BUTTON =
      formatIconButton(
          MessageUtils.ACCEPT, "Yes", NamedTextColor.DARK_GREEN, "/yes", "Click to vote yes!");

  private static final Component NO_BUTTON =
      formatIconButton(MessageUtils.DENY, "No", NamedTextColor.RED, "/no", "Click to vote no!");

  private static final Component VOTE_BUTTONS =
      text().append(YES_BUTTON).appendSpace().appendSpace().appendSpace().append(NO_BUTTON).build();

  private static Component formatIconButton(
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

  private static Component formatButton(
      String text, NamedTextColor color, String command, String hover) {
    return text()
        .append(text("[", NamedTextColor.GRAY))
        .appendSpace()
        .append(text(text, color))
        .append(text("]", NamedTextColor.GRAY))
        .appendSpace()
        .hoverEvent(HoverEvent.showText(text(hover, NamedTextColor.GRAY)))
        .clickEvent(ClickEvent.runCommand(command))
        .build();
  }

  public void sendPollDetails(CommandAudience audience) {
    if (getPoll() == null) {
      audience.sendWarning(text("No active poll found!"));
      return;
    }

    // Calc duration
    Duration duration =
        getPoll().getEndTime() != null
            ? Duration.between(getPoll().getStartTime(), getPoll().getEndTime())
            : null;

    sendDetails(
        audience,
        "Poll Details",
        getPoll().getQuestion(),
        duration,
        getPoll().getEndAction(),
        false);

    if (getPoll() instanceof TimedPoll) {
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
    audience.sendMessage(FOOTER);
  }

  public void sendBuilderDetails(CommandAudience audience) {
    sendDetails(
        audience,
        "Poll Setup Details",
        getBuilder().getQuestion(),
        getBuilder().getDuration(),
        getBuilder().getEndAction(),
        !builder.canBuild());

    if (builder.canBuild()) {
      Component buttons =
          text()
              .append(
                  formatButton("Start", NamedTextColor.GREEN, "/poll start", "Click to start poll"))
              .appendSpace()
              .append(
                  formatButton("Reset", NamedTextColor.RED, "/poll reset", "Click to reset values"))
              .build();

      audience.sendMessage(buttons);
      audience.sendMessage(FOOTER);
    }
  }

  private void sendDetails(
      CommandAudience audience,
      String title,
      Component question,
      Duration duration,
      EndAction action,
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

    if (question == null) {
      question = action.getDefaultQuestion();
    }

    audience.sendMessage(formatCategoryDetail("Question", question));
    audience.sendMessage(formatCategoryDetail("Duration", durationComponent));
    audience.sendMessage(formatCategoryDetail("End Action", endAction));

    audience.sendMessage(text(""));

    if (footer) {
      audience.sendMessage(FOOTER);
    }
  }

  private static final Component FOOTER =
      TextFormatter.horizontalLine(NamedTextColor.GRAY, TextFormatter.MAX_CHAT_WIDTH);

  private Component formatCategoryDetail(String category, Component value) {
    return text()
        .append(text(category, NamedTextColor.GOLD, TextDecoration.BOLD))
        .append(text(": ", NamedTextColor.GRAY))
        .append(value)
        .build();
  }

  // Yoinked out of MatchCountdown
  private TextColor urgencyColor(Duration remaining) {
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

  private boolean shouldShowAlert(Duration remaining) {
    long secondsLeft = remaining.getSeconds();
    return secondsLeft > 0
        && (secondsLeft % 300 == 0
            || // every 5 minutes
            (secondsLeft % 60 == 0 && secondsLeft <= 300)
            || // every minute for the last 5 minutes
            (secondsLeft % 10 == 0 && secondsLeft <= 30)
            || // every 10 seconds for the last 30 seconds
            secondsLeft <= 5); // every second for the last 5 seconds
  }
}

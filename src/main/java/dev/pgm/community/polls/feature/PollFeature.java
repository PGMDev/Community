package dev.pgm.community.polls.feature;

import static net.kyori.adventure.text.Component.text;

import dev.pgm.community.Community;
import dev.pgm.community.feature.FeatureBase;
import dev.pgm.community.polls.Poll;
import dev.pgm.community.polls.PollBuilder;
import dev.pgm.community.polls.PollComponents;
import dev.pgm.community.polls.PollConfig;
import dev.pgm.community.polls.PollEditAlerter;
import dev.pgm.community.polls.events.PollEndEvent;
import dev.pgm.community.polls.events.PollStartEvent;
import dev.pgm.community.polls.events.PollVoteEvent;
import dev.pgm.community.polls.types.TimedPoll;
import dev.pgm.community.utils.CommandAudience;
import java.time.Duration;
import java.time.Instant;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import tc.oc.pgm.util.Audience;

public class PollFeature extends FeatureBase implements PollEditAlerter, PollComponents {

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

      for (Player player : Bukkit.getOnlinePlayers()) {
        Audience viewer = Audience.get(player);
        boolean hasVoted = timedPoll.hasVoted(player);

        if (shouldShowVoteReminder(hasVoted, timeLeft)) {
          Component alert = createVoteReminderBroadcast(poll, timeLeft, hasVoted);
          viewer.sendMessage(alert);
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
    sendPollBroadcast(poll);

    // Call start event
    Community.get().callEvent(new PollStartEvent(poll));
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

    if (poll.getEndTime() == null) {
      poll.setEndTime(Instant.now());
    }

    long yesVotes = poll.getYesVotesCount();
    long noVotes = poll.getNoVotesCount();
    long totalVotes = yesVotes + noVotes;

    double desiredThresholdPercentage = poll.getRequiredThreshold().getValue();
    long requiredThreshold = Math.round(totalVotes * desiredThresholdPercentage);

    boolean majorityOption = yesVotes >= requiredThreshold;

    sendPollResults(poll, yesVotes, noVotes, majorityOption);

    if (majorityOption) {
      Player creatorPlayer = Bukkit.getPlayer(poll.getCreator());
      poll.getEndAction().execute(creatorPlayer);
    }

    // Call end event
    Community.get().callEvent(new PollEndEvent(poll));

    poll = null; // Reset the poll
    resetBuilder(); // Reset builder so values are clean
  }

  public void vote(CommandAudience viewer, Player sender, String option) {
    if (poll == null) {
      viewer.sendWarning(text("There's no poll to vote for at this time!"));
      return;
    }

    if (!poll.vote(sender, option)) {
      viewer.sendWarning(text("You have already voted for this poll!"));
      return;
    }

    // Call vote event
    Community.get().callEvent(new PollVoteEvent(sender, poll));

    viewer.sendMessage(
        text("Thanks for voting! The results will be announced soon.", NamedTextColor.GREEN));
  }

  private boolean shouldShowVoteReminder(boolean hasVoted, Duration remaining) {
    long secondsLeft = remaining.getSeconds();
    if (hasVoted) {
      return secondsLeft <= 5;
    } else {
      // If the user has not voted, we'll remind them at common intervals.
      return secondsLeft > 0
          && ((secondsLeft % 300 == 0)
              || // every 5 minutes
              (secondsLeft % 60 == 0 && secondsLeft <= 300)
              || // every minute for the last 5 minutes
              (secondsLeft % 10 == 0 && secondsLeft <= 30)
              || // every 10 seconds for the last 30 seconds
              secondsLeft <= 5); // every second for the last 5 seconds
    }
  }
}

package dev.pgm.community.polls;

import static net.kyori.adventure.text.Component.text;

import dev.pgm.community.polls.ending.EndAction;
import dev.pgm.community.polls.ending.types.CommandEndAction;
import dev.pgm.community.polls.ending.types.KickPlayerAction;
import dev.pgm.community.polls.ending.types.NullEndAction;
import dev.pgm.community.polls.ending.types.SetNextAction;
import dev.pgm.community.polls.types.NormalPoll;
import dev.pgm.community.polls.types.TimedPoll;
import dev.pgm.community.utils.CommandAudience;
import java.time.Duration;
import java.util.UUID;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.map.MapInfo;

public class PollBuilder {

  private PollEditAlerter alert; // Used to broadcast changes to values

  public PollBuilder(PollEditAlerter alert) {
    this.alert = alert;
  }

  // Required
  private Component question; // If null & not NullEndAction, then we customize the message on build
  private UUID creator; // Only set when built

  // Optional
  private Duration duration; // null for open-ended
  private EndAction endAction = new NullEndAction();

  public PollBuilder question(CommandAudience sender, String question) {
    this.question = (question != null ? text(question) : null);
    alert.broadcastChange(sender, "Poll Question", question);
    return this;
  }

  public PollBuilder creator(UUID creator) {
    this.creator = creator;
    return this;
  }

  public PollBuilder duration(CommandAudience sender, Duration duration) {
    this.duration = duration;
    alert.broadcastChange(sender, "Poll Duration", duration);
    return this;
  }

  public PollBuilder command(CommandAudience sender, String command) {
    alert.broadcastChange(sender, "Poll Command", command);

    if (command == null || command.isEmpty()) {
      endAction = new NullEndAction();
      return this;
    }

    this.endAction = new CommandEndAction(command);
    return this;
  }

  public PollBuilder map(CommandAudience sender, MapInfo map) {
    alert.broadcastChange(sender, "Poll Map", map);

    if (map == null) {
      endAction = new NullEndAction();
      return this;
    }

    this.endAction = new SetNextAction(map);
    return this;
  }

  public PollBuilder kickPlayer(CommandAudience sender, Player target) {
    alert.broadcastChange(sender, "Poll Kick Target", target.getName());

    if (target == null || !target.isOnline()) {
      endAction = new NullEndAction();
      return this;
    }

    this.endAction = new KickPlayerAction(target.getUniqueId());
    return this;
  }

  public Poll build() {
    Poll poll;

    if (question == null) {
      question = endAction.getDefaultQuestion();
    }

    if (duration != null) {
      poll = new TimedPoll(question, creator, endAction, duration);
    } else {
      poll = new NormalPoll(question, creator, endAction);
    }
    return poll;
  }

  @Nullable
  public Component getQuestion() {
    return question;
  }

  public Duration getDuration() {
    return duration;
  }

  public EndAction getEndAction() {
    return endAction;
  }

  public boolean canBuild() {
    return question != null || !(endAction instanceof NullEndAction);
  }
}

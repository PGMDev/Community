package dev.pgm.community.polls;

import static net.kyori.adventure.text.Component.text;
import static tc.oc.pgm.util.text.TextException.exception;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import dev.pgm.community.mutations.MutationType;
import dev.pgm.community.polls.ending.EndAction;
import dev.pgm.community.polls.ending.types.CommandEndAction;
import dev.pgm.community.polls.ending.types.KickPlayerEndAction;
import dev.pgm.community.polls.ending.types.MapEndAction;
import dev.pgm.community.polls.ending.types.MutationEndAction;
import dev.pgm.community.polls.ending.types.NullEndAction;
import dev.pgm.community.polls.types.MultiChoicePoll;
import dev.pgm.community.polls.types.SingleChoicePoll;
import dev.pgm.community.utils.CommandAudience;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import tc.oc.pgm.api.map.MapInfo;

public class PollBuilder {

  private static final int MAX_POLL_OPTIONS = 4;

  private PollEditAlerter alert; // Used to broadcast changes to values

  // Required
  private Component question;
  private UUID creator;
  private PollThreshold threshold = PollThreshold.SIMPLE;
  private Duration duration;

  // Optional
  private Set<EndAction> endActions = Sets.newHashSet();

  public PollBuilder(PollConfig config, PollEditAlerter alert) {
    this.alert = alert;
    this.duration = config.getDuration();
    this.threshold = config.getThreshold();
  }

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
    duration = duration.abs();
    this.duration = duration;
    alert.broadcastChange(sender, "Poll Duration", duration);
    return this;
  }

  public void addEndAction(CommandAudience sender, EndAction action) {
    if (endActions.size() < MAX_POLL_OPTIONS) {
      if (!endActions.add(action)) {
        throw exception("This option is already present! Use /poll to manage all options");
      }
    } else {
      throw exception("Maximum number of poll options reached!");
    }

    int total = endActions.size();
    String type = action.getTypeName();

    if (total > 1) {
      type += " (" + total + " / " + MAX_POLL_OPTIONS + ")";
    }

    alert.broadcastChange(sender, type, action.getPreviewValue());
  }

  public PollBuilder command(CommandAudience sender, String command) {
    addEndAction(sender, new CommandEndAction(command));
    return this;
  }

  public PollBuilder map(CommandAudience sender, MapInfo map) {
    addEndAction(sender, new MapEndAction(map));
    return this;
  }

  public PollBuilder kickPlayer(CommandAudience sender, UUID playerId) {
    addEndAction(sender, new KickPlayerEndAction(playerId));
    return this;
  }

  public PollBuilder mutation(CommandAudience sender, MutationType mutation) {
    addEndAction(sender, new MutationEndAction(mutation));
    return this;
  }

  public PollBuilder threshold(CommandAudience sender, PollThreshold threshold) {
    alert.broadcastChange(sender, "Poll Threshold", threshold);

    if (threshold == null) {
      this.threshold = PollThreshold.SIMPLE;
      return this;
    }

    this.threshold = threshold;
    return this;
  }

  private EndAction getAction(String option) {
    for (EndAction action : endActions) {
      if (action.getValue().equalsIgnoreCase(option)) {
        return action;
      }
    }

    return null;
  }

  public boolean remove(CommandAudience sender, String option) {
    EndAction action = getAction(option);

    if (action == null) {
      sender.sendWarning(
          text()
              .append(text("No option called '"))
              .append(text(option, NamedTextColor.AQUA))
              .append(text("' found."))
              .build());
      return false;
    }

    endActions.remove(action);

    alert.broadcastChange(sender, action.getTypeName() + " Removed", action.getPreviewValue());
    return true;
  }

  public Poll build() {
    Poll poll;

    List<EndAction> actions = Lists.newArrayList();

    if (endActions.isEmpty()) {
      endActions.add(new NullEndAction());
    }

    for (EndAction action : endActions) {
      actions.add(action);
    }

    if (question == null) {
      question = generateQuestion(actions);
    }

    if (actions.size() == 1) {
      poll =
          new SingleChoicePoll(question, creator, threshold, duration, actions.iterator().next());
    } else if (endActions.size() > 1) {
      threshold = PollThreshold.SIMPLE;
      poll = new MultiChoicePoll(question, creator, threshold, duration, actions);
    } else {
      throw exception("Unable to create new poll! No end action found.");
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

  public Set<EndAction> getEndAction() {
    return endActions;
  }

  public PollThreshold getThreshold() {
    return threshold;
  }

  public boolean canBuild() {
    return question != null || !endActions.isEmpty();
  }

  public static Component generateQuestion(List<EndAction> actions) {
    if (actions.isEmpty()) {
      return new NullEndAction().getDefaultQuestion();
    }

    if (actions.size() == 1) {
      return actions.iterator().next().getDefaultQuestion();
    }

    Set<Class<? extends EndAction>> uniqueActionTypes = new HashSet<>();
    for (EndAction action : actions) {
      uniqueActionTypes.add(action.getClass());
      if (uniqueActionTypes.size() > 1) {
        return text("What should we do?", NamedTextColor.WHITE);
      }
    }

    if (uniqueActionTypes.size() == 1) {
      Class<? extends EndAction> singleActionType = uniqueActionTypes.iterator().next();
      NamedTextColor textColor = NamedTextColor.WHITE;
      String actionTypeName = "";
      String questionSuffix = "";

      if (singleActionType.equals(CommandEndAction.class)) {
        actionTypeName = "command";
        textColor = NamedTextColor.AQUA;
        questionSuffix = "execute?";
      } else if (singleActionType.equals(KickPlayerEndAction.class)) {
        actionTypeName = "player";
        textColor = NamedTextColor.GOLD;
        questionSuffix = "kick?";
      } else if (singleActionType.equals(MutationEndAction.class)) {
        actionTypeName = "mutation";
        textColor = NamedTextColor.GREEN;
        questionSuffix = "toggle?";
      } else if (singleActionType.equals(MapEndAction.class)) {
        actionTypeName = "map";
        textColor = NamedTextColor.YELLOW;
        questionSuffix = "play next?";
      }

      return text()
          .append(text("Which "))
          .append(text(actionTypeName, textColor))
          .append(text(" should we "))
          .append(text(questionSuffix))
          .color(NamedTextColor.WHITE)
          .build();
    }

    return text("What should we do?", NamedTextColor.WHITE);
  }
}

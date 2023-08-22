package dev.pgm.community.polls.types;

import static net.kyori.adventure.text.Component.text;

import com.google.common.collect.Maps;
import dev.pgm.community.polls.PollThreshold;
import dev.pgm.community.polls.ending.EndAction;
import dev.pgm.community.polls.response.MultiChoiceResponseConverter;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class MultiChoicePoll extends BasicPoll {

  private final List<EndAction> options;
  private final Map<UUID, Integer> votes;

  public MultiChoicePoll(
      Component question,
      UUID creator,
      PollThreshold threshold,
      Duration duration,
      List<EndAction> options) {
    super(question, creator, threshold, duration);
    this.options = options;
    this.votes = Maps.newHashMap();
  }

  @Override
  public void tallyVotes() {
    int[] optionVoteCounts = new int[options.size()];

    for (int optionIndex : votes.values()) {
      if (optionIndex >= 0 && optionIndex < options.size()) {
        optionVoteCounts[optionIndex]++;
      }
    }

    int maxVotes = 0;
    int winningOption = -1;

    for (int i = 0; i < optionVoteCounts.length; i++) {
      if (optionVoteCounts[i] > maxVotes) {
        maxVotes = optionVoteCounts[i];
        winningOption = i;
      }
    }

    if (winningOption != -1) {
      EndAction winningAction = options.get(winningOption);
      Player creatorPlayer = Bukkit.getPlayer(getCreator());
      if (creatorPlayer != null) {
        winningAction.execute(creatorPlayer);
      }

      Map<EndAction, Integer> tallyVotes = Maps.newHashMap();
      for (int i = 0; i < optionVoteCounts.length; i++) {
        tallyVotes.put(options.get(i), optionVoteCounts[i]);
      }

      sendMultiChoicePollResults(this, tallyVotes, winningAction);
    }
  }

  @Override
  public List<EndAction> getEndAction() {
    return options;
  }

  @Override
  public boolean vote(Player player, String option) {
    int selectedOption = MultiChoiceResponseConverter.convert(option, options);

    if (selectedOption != -1 && !hasVoted(player)) {
      votes.put(player.getUniqueId(), selectedOption);
      return true;
    }

    return false;
  }

  @Override
  public long getTotalVotes() {
    return votes.size();
  }

  @Override
  public boolean hasVoted(Player player) {
    return votes.containsKey(player.getUniqueId());
  }

  @Override
  public Component getVoteButtons(boolean compact) {
    Component buttons = generateVoteButtons(options, isMixed(options));

    if (compact) {
      return text()
          .appendNewline()
          .append(text("Vote: ", NamedTextColor.GRAY))
          .append(buttons)
          .build();
    }

    return buttons;
  }

  public static boolean isMixed(List<EndAction> actions) {
    if (actions.isEmpty()) {
      return false;
    }

    Set<Class<? extends EndAction>> uniqueActionTypes = new HashSet<>();
    for (EndAction action : actions) {
      uniqueActionTypes.add(action.getClass());
      if (uniqueActionTypes.size() > 1) {
        return true;
      }
    }

    return false;
  }
}

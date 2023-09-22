package dev.pgm.community.polls.types;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dev.pgm.community.polls.PollThreshold;
import dev.pgm.community.polls.ending.EndAction;
import dev.pgm.community.polls.response.SingleChoiceResponseConverter;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class SingleChoicePoll extends BasicPoll {

  private final EndAction action;
  private final Map<UUID, Boolean> votes;

  public SingleChoicePoll(
      Component question,
      UUID creator,
      PollThreshold threshold,
      Duration duration,
      EndAction action) {
    super(question, creator, threshold, duration);
    this.action = action;
    this.votes = Maps.newHashMap();
  }

  @Override
  public void tallyVotes() {
    long yesVotes = getYesVotesCount();
    long noVotes = getNoVotesCount();
    long totalVotes = yesVotes + noVotes;

    double desiredThresholdPercentage = getRequiredThreshold().getValue();
    long requiredThreshold = Math.round(totalVotes * desiredThresholdPercentage);

    boolean majorityOption = yesVotes >= requiredThreshold;

    sendBooleanPollResults(this, yesVotes, noVotes, majorityOption);

    if (majorityOption) {
      Player creatorPlayer = Bukkit.getPlayer(getCreator());
      action.execute(creatorPlayer);
    }
  }

  @Override
  public List<EndAction> getEndAction() {
    return Lists.newArrayList(action);
  }

  @Override
  public long getTotalVotes() {
    return getOnlineVotes().size();
  }

  @Override
  public boolean vote(Player player, String option) {
    boolean vote = SingleChoiceResponseConverter.convert(option);

    if (!hasVoted(player)) {
      votes.put(player.getUniqueId(), vote);
      return true;
    }

    return false;
  }

  @Override
  public boolean hasVoted(Player player) {
    return votes.containsKey(player.getUniqueId());
  }

  @Override
  public Component getVoteButtons(boolean compact) {
    return getBooleanVoteButtons(compact);
  }

  public long getYesVotesCount() {
    return getOnlineVotes().values().stream().filter(Boolean::booleanValue).count();
  }

  public long getNoVotesCount() {
    return getOnlineVotes().values().stream().filter(vote -> !vote).count();
  }

  private Map<UUID, Boolean> getOnlineVotes() {
    return votes.entrySet().stream()
        .filter(
            entry -> {
              Player player = Bukkit.getPlayer(entry.getKey());
              return player != null && player.isOnline();
            })
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }
}

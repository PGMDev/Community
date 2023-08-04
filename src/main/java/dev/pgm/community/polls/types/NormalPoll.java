package dev.pgm.community.polls.types;

import static net.kyori.adventure.text.Component.text;

import com.google.common.collect.Maps;
import dev.pgm.community.polls.Poll;
import dev.pgm.community.polls.ending.EndAction;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class NormalPoll implements Poll {

  private final Component question;
  private final UUID creator;
  private final Instant startTime;
  protected Instant endTime;
  private EndAction action;
  private final Map<UUID, Boolean> votes;

  public NormalPoll(Component question, UUID creator, EndAction action) {
    this.question = question;
    this.creator = creator;
    this.action = action;
    this.startTime = Instant.now();
    this.votes = Maps.newHashMap();
  }

  @Override
  public Component getQuestion() {
    if (question == null) {
      return text("");
    }
    return question;
  }

  @Override
  public UUID getCreator() {
    return creator;
  }

  @Override
  public Instant getStartTime() {
    return startTime;
  }

  @Override
  public Instant getEndTime() {
    return endTime;
  }

  @Override
  public void setEndTime(Instant time) {
    this.endTime = time;
  }

  @Override
  public boolean isRunning() {
    return endTime == null || endTime.isAfter(Instant.now());
  }

  @Override
  public EndAction getEndAction() {
    return action;
  }

  @Override
  public boolean vote(Player player, boolean option) {
    if (!hasVoted(player)) {
      votes.put(player.getUniqueId(), option);
      return true;
    }
    return false;
  }

  public Map<UUID, Boolean> getVotes() {
    return votes;
  }

  public Map<UUID, Boolean> getOnlineVotes() {
    return getVotes().entrySet().stream()
        .filter(
            entry -> {
              Player player = Bukkit.getPlayer(entry.getKey());
              return player != null && player.isOnline();
            })
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  @Override
  public long getYesVotesCount() {
    return getOnlineVotes().values().stream().filter(Boolean::booleanValue).count();
  }

  @Override
  public long getNoVotesCount() {
    return getOnlineVotes().values().stream().filter(vote -> !vote).count();
  }

  @Override
  public long getTotalVotes() {
    return getOnlineVotes().size();
  }

  public boolean hasVoted(Player player) {
    return votes.containsKey(player.getUniqueId());
  }

  public boolean getPlayerVote(Player player) {
    return votes.getOrDefault(player.getUniqueId(), false);
  }
}

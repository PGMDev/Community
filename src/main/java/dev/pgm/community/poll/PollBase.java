package dev.pgm.community.poll;

import com.google.common.collect.Maps;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public abstract class PollBase implements Poll {

  private String text;
  private Duration length;
  private boolean active;
  private UUID executor;

  private @Nullable Instant startTime;

  private Map<UUID, Boolean> votes;

  public PollBase(String text, Duration length, boolean active, UUID playerId) {
    this.text = text;
    this.length = length;
    this.active = active;
    this.executor = playerId;
    this.startTime = null;
    this.votes = Maps.newHashMap();
  }

  @Override
  public String getText() {
    return text;
  }

  @Override
  public Player getExecutor() {
    return Bukkit.getPlayer(executor);
  }

  @Override
  public Duration getLength() {
    return length;
  }

  @Override
  public boolean isActive() {
    return active;
  }

  @Override
  public void setText(String text) {
    this.text = text;
  }

  @Override
  public void setExecutor(UUID playerId) {
    this.executor = playerId;
  }

  @Override
  public void setLength(Duration length) {
    this.length = length;
  }

  @Override
  public Duration getTimeLeft() {
    if (startTime == null) return Duration.ZERO;
    return Duration.ofSeconds(
        Math.max(
            getLength().getSeconds() - Duration.between(startTime, Instant.now()).getSeconds(), 0));
  }

  @Override
  public void start() {
    this.active = true;
    this.startTime = Instant.now();
  }

  @Override
  public void complete() {
    this.active = false;
  }

  @Override
  public void vote(Player player, boolean yay) {
    this.votes.put(player.getUniqueId(), yay);
  }

  @Override
  public boolean getOutcome() {
    return getVoteTally(true) > getVoteTally(false);
  }

  @Override
  public int getVoteTally(boolean yes) {
    return Math.toIntExact(votes.values().stream().filter(vote -> yes ? vote : !vote).count());
  }
}

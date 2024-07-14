package dev.pgm.community.nick;

import java.time.Instant;
import java.util.UUID;

public class NickImpl implements Nick {

  private UUID playerId;
  private String nickName;
  private Instant date;
  private boolean enabled;

  public NickImpl(UUID playerId, String nickName, Instant date, boolean enabled) {
    this.playerId = playerId;
    this.nickName = nickName;
    this.date = date;
    this.enabled = enabled;
  }

  @Override
  public String getName() {
    return nickName;
  }

  @Override
  public void setName(String name) {
    this.nickName = name;
    this.enabled = true;
    this.date = Instant.now();
  }

  @Override
  public UUID getPlayerId() {
    return playerId;
  }

  @Override
  public Instant getDateSet() {
    return date;
  }

  @Override
  public boolean isEnabled() {
    return !nickName.isEmpty() && enabled;
  }

  @Override
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }
}

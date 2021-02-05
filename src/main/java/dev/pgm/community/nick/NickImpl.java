package dev.pgm.community.nick;

import java.time.Instant;
import java.util.UUID;

public class NickImpl implements Nick {

  private UUID nickId;
  private UUID playerId;
  private String nickName;
  private Instant date;
  private boolean valid;
  private boolean enabled;

  public NickImpl(
      UUID nickId, UUID playerId, String nickName, Instant date, boolean valid, boolean enabled) {
    this.nickId = nickId;
    this.playerId = playerId;
    this.nickName = nickName;
    this.date = date;
    this.valid = valid;
    this.enabled = enabled;
  }

  @Override
  public UUID getNickId() {
    return nickId;
  }

  @Override
  public String getNickName() {
    return nickName;
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
  public boolean isValid() {
    return valid;
  }

  @Override
  public void setValid(boolean valid) {
    this.valid = valid;
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }
}

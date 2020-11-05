package dev.pgm.community.users;

import dev.pgm.community.Community;
import java.time.Instant;
import java.util.UUID;

public class UserProfileImpl implements UserProfile {

  private UUID playerId;
  private String username;
  private Instant firstLogin;
  private Instant lastLogin;
  private int joinCount;
  private String serverName;

  public UserProfileImpl(UUID playerId, String username) {
    this(
        playerId,
        username,
        Instant.now(),
        Instant.now(),
        1,
        Community.get().getServerConfig().getServerId());
  }

  public UserProfileImpl(
      UUID playerId,
      String username,
      Instant firstLogin,
      Instant lastLogin,
      int joinCount,
      String server) {
    this.playerId = playerId;
    this.username = username;
    this.firstLogin = firstLogin;
    this.lastLogin = lastLogin;
    this.joinCount = joinCount;
    this.serverName = server;
  }

  @Override
  public UUID getId() {
    return playerId;
  }

  @Override
  public String getUsername() {
    return username;
  }

  @Override
  public Instant getFirstLogin() {
    return firstLogin;
  }

  @Override
  public Instant getLastLogin() {
    return lastLogin;
  }

  @Override
  public int getJoinCount() {
    return joinCount;
  }

  @Override
  public String getServerName() {
    return serverName;
  }

  @Override
  public void setUsername(String username) {
    this.username = username;
  }

  @Override
  public void setFirstLogin(Instant now) {
    this.firstLogin = now;
  }

  @Override
  public void setLastLogin(Instant now) {
    this.lastLogin = now;
  }

  @Override
  public void incJoinCount() {
    this.joinCount++;
  }
}

package dev.pgm.community.requests;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import tc.oc.pgm.api.map.MapInfo;

public class RequestProfile {

  private final UUID playerId;
  private Instant lastRequestTime;
  private String lastRequestMap;

  private Instant lastSponsorTime;
  private String lastSponsorMap;

  private int sponsorTokens;
  private Instant lastTokenRefreshTime;

  public RequestProfile(UUID playerId) {
    this(playerId, null, null, null, null, 0, null);
  }

  public RequestProfile(
      UUID playerId,
      Instant lastRequestTime,
      String lastRequestMap,
      Instant lastSponsorTime,
      String lastSponsorMap,
      int sponsorTokens,
      Instant lastTokenRefreshTime) {
    this.playerId = playerId;
    this.lastRequestTime = lastRequestTime;
    this.lastRequestMap = lastRequestMap;
    this.lastSponsorTime = lastSponsorTime;
    this.lastSponsorMap = lastSponsorMap;
    this.sponsorTokens = sponsorTokens;
    this.lastTokenRefreshTime = lastTokenRefreshTime;
  }

  public int award(int amount) {
    this.sponsorTokens = Math.max(0, sponsorTokens + amount);
    return sponsorTokens;
  }

  public void request(MapInfo map) {
    this.lastRequestMap = map.getId();
    this.lastRequestTime = Instant.now();
  }

  public void sponsor(MapInfo map) {
    this.lastSponsorMap = map.getId();
    this.lastSponsorTime = Instant.now();
  }

  public Instant getLastRequestTime() {
    return lastRequestTime;
  }

  public String getLastRequestMap() {
    return lastRequestMap;
  }

  public Instant getLastSponsorTime() {
    return lastSponsorTime;
  }

  public String getLastSponsorMap() {
    return lastSponsorMap;
  }

  public int getSponsorTokens() {
    return sponsorTokens;
  }

  public Instant getLastTokenRefreshTime() {
    return lastTokenRefreshTime;
  }

  public void refreshTokens(int amount) {
    award(amount);
    this.lastTokenRefreshTime = Instant.now();
  }

  public UUID getPlayerId() {
    return playerId;
  }

  public boolean hasWeekElapsed() {
    if (lastTokenRefreshTime == null) return true;
    return Duration.between(lastTokenRefreshTime, Instant.now()).toDays() > 7;
  }

  public boolean hasDayElapsed() {
    if (lastTokenRefreshTime == null) return true;
    return Duration.between(lastTokenRefreshTime, Instant.now()).toHours() > 24;
  }

  @Override
  public String toString() {
    return String.format(
        "RequestProfile{id = %d, tokens = %d, requestMap = %s, sponsorMap = %s, lastRequest = %s, lastSponsor = %s, lastRefresh = %s}",
        getPlayerId().toString(),
        getSponsorTokens(),
        getLastRequestMap(),
        getLastSponsorMap(),
        getLastRequestTime().toString(),
        getLastSponsorTime().toString(),
        getLastTokenRefreshTime().toString());
  }
}

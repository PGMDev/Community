package dev.pgm.community.requests;

import java.util.UUID;
import tc.oc.pgm.api.map.MapInfo;

public class SponsorRequest {

  private UUID playerId;
  private MapInfo map;

  public SponsorRequest(UUID playerId, MapInfo map) {
    this.playerId = playerId;
    this.map = map;
  }

  public UUID getPlayerId() {
    return playerId;
  }

  public MapInfo getMap() {
    return map;
  }
}

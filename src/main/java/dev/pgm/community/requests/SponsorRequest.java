package dev.pgm.community.requests;

import java.util.UUID;
import tc.oc.pgm.api.map.MapInfo;

public class SponsorRequest {

  private UUID playerId;
  private MapInfo map;
  private boolean canRefund;

  public SponsorRequest(UUID playerId, MapInfo map, boolean canRefund) {
    this.playerId = playerId;
    this.map = map;
    this.canRefund = canRefund;
  }

  public UUID getPlayerId() {
    return playerId;
  }

  public MapInfo getMap() {
    return map;
  }

  public boolean canRefund() {
    return canRefund;
  }
}

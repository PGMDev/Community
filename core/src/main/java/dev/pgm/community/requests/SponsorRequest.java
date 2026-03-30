package dev.pgm.community.requests;

import java.util.UUID;
import tc.oc.pgm.api.map.MapInfo;

public record SponsorRequest(UUID playerId, MapInfo map, boolean canRefund) {}

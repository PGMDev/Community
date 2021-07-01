package dev.pgm.community.requests.feature;

import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.text;
import static tc.oc.pgm.util.text.PlayerComponent.player;

import com.google.common.collect.Lists;
import dev.pgm.community.requests.SponsorRequest;
import dev.pgm.community.users.feature.UsersFeature;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import tc.oc.pgm.api.integration.Integration;
import tc.oc.pgm.api.integration.RequestIntegration;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.util.named.NameStyle;

public class PGMRequestIntegration implements RequestIntegration {

  private final RequestFeature requests;
  private final UsersFeature users;

  public PGMRequestIntegration(RequestFeature requests, UsersFeature users) {
    this.requests = requests;
    this.users = users;
    Integration.setRequestIntegration(this);
  }

  @Override
  public List<Component> getExtraMatchInfoLines(MapInfo map) {
    if (isSponsor(map)) {
      UUID playerId = requests.getCurrentSponsor().getPlayerId();
      return Lists.newArrayList(
          text()
              .append(newline())
              .append(text(" Sponsored by "))
              .append(player(playerId, users.getUsername(playerId), NameStyle.FANCY))
              .color(NamedTextColor.GRAY)
              .build());
    }
    return Lists.newArrayList();
  }

  @Override
  public boolean isSponsor(MapInfo map) {
    SponsorRequest request = requests.getCurrentSponsor();
    return request != null && request.getMap().equals(map);
  }
}

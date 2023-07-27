package dev.pgm.community.requests.feature;

import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.text;
import static tc.oc.pgm.util.player.PlayerComponent.player;

import dev.pgm.community.requests.SponsorRequest;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.rotation.vote.book.VotingBookCreatorImpl;
import tc.oc.pgm.util.named.NameStyle;

public class SponsorVotingBookCreator extends VotingBookCreatorImpl {

  private final RequestFeature manager;

  public SponsorVotingBookCreator(RequestFeature manager) {
    this.manager = manager;
  }

  @Override
  public ComponentLike getHover(MatchPlayer viewer, MapInfo map, boolean voted) {
    ComponentLike originalHover = super.getHover(viewer, map, voted);

    // Add sponsor hover if available
    SponsorRequest sponsor = manager.getCurrentSponsor();
    if (sponsor != null && sponsor.getMap().equals(map)) {
      return text()
          .append(originalHover)
          .append(newline())
          .append(text("+ ", NamedTextColor.GREEN, TextDecoration.BOLD))
          .append(text("Sponsored by ", NamedTextColor.GRAY))
          .append(player(sponsor.getPlayerId(), NameStyle.FANCY))
          .build();
    }

    return originalHover;
  }
}

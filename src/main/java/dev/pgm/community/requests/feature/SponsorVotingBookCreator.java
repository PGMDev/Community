package dev.pgm.community.requests.feature;

import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.event.ClickEvent.runCommand;
import static net.kyori.adventure.text.event.HoverEvent.showText;
import static tc.oc.pgm.util.player.PlayerComponent.player;

import dev.pgm.community.requests.SponsorRequest;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapTag;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.rotation.vote.book.VotingBookCreator;
import tc.oc.pgm.util.named.NameStyle;

public class SponsorVotingBookCreator implements VotingBookCreator {

  private static final String SYMBOL_IGNORE = "\u2715"; // ✕
  private static final String SYMBOL_VOTED = "\u2714"; // ✔

  private final RequestFeature manager;

  public SponsorVotingBookCreator(RequestFeature manager) {
    this.manager = manager;
  }

  private Component getHover(MapInfo map) {
    TextComponent.Builder hover = text();
    // Map tags
    hover.append(
        text(
            map.getTags().stream().map(MapTag::toString).collect(Collectors.joining(" ")),
            NamedTextColor.YELLOW));

    // Add sponsor hover
    SponsorRequest sponsor = manager.getCurrentSponsor();
    if (sponsor != null && sponsor.getMap().equals(map)) {
      hover
          .append(
              text()
                  .append(newline())
                  .append(text("+ ", NamedTextColor.GREEN, TextDecoration.BOLD))
                  .append(text("Sponsored by ", NamedTextColor.GRAY)))
          .append(player(sponsor.getPlayerId(), NameStyle.FANCY));
    }
    return hover.build();
  }

  @Override
  public Component getMapBookComponent(MatchPlayer viewer, MapInfo map, boolean voted) {
    TextComponent.Builder text = text();
    text.append(
        text(
            voted ? SYMBOL_VOTED : SYMBOL_IGNORE,
            voted ? NamedTextColor.DARK_GREEN : NamedTextColor.DARK_RED));
    text.append(text(" ").decoration(TextDecoration.BOLD, !voted));
    text.append(text(map.getName(), NamedTextColor.GOLD, TextDecoration.BOLD));
    text.hoverEvent(showText(getHover(map)));
    text.clickEvent(runCommand("/votenext -o " + map.getName())); // Fix 1px symbol diff
    return text.build();
  }
}

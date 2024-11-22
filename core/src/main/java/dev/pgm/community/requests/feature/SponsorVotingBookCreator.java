package dev.pgm.community.requests.feature;

import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.text;
import static tc.oc.pgm.util.player.PlayerComponent.player;

import dev.pgm.community.Community;
import dev.pgm.community.requests.RequestConfig;
import dev.pgm.community.requests.RequestProfile;
import dev.pgm.community.requests.SponsorRequest;
import dev.pgm.community.requests.supervotes.SuperVoteComponents;
import dev.pgm.community.utils.MessageUtils;
import dev.pgm.community.utils.ranks.RanksConfig.Rank;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
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
          .append(text("+ ", NamedTextColor.YELLOW, TextDecoration.BOLD))
          .append(text("Sponsored by ", NamedTextColor.GRAY))
          .append(player(sponsor.getPlayerId(), NameStyle.FANCY))
          .build();
    }

    return originalHover;
  }

  @Override
  public Component getMapBookFooter(MatchPlayer viewer) {
    boolean active = manager.isSuperVoteActive(viewer.getBukkit());
    int standard = manager.getStandardExtraVoteLevel(viewer.getBukkit());
    int superLevel = manager.getMultipliedExtraVoteLevel(viewer.getBukkit());

    // a 0 should display as 1x
    if (standard < 1) standard = 1;

    RequestProfile profile = manager.getCached(viewer.getId());
    int remaining = profile != null ? profile.getSuperVotes() : 0;
    boolean canUse = manager.canSuperVote(viewer.getBukkit()) && remaining > 0;

    Component votePrefix = text()
        .append(MessageUtils.VOTE)
        .hoverEvent(HoverEvent.showText(SuperVoteComponents.getSuperVoteBalance(remaining)))
        .build();

    TextComponent.Builder builder = text()
        .appendNewline()
        .appendNewline()
        .append(votePrefix)
        .appendSpace()
        .append(
            active
                ? getSuperVoteActiveComponent()
                : getSuperVoteButtonComponent(canUse, remaining, superLevel))
        .appendNewline()
        .appendNewline()
        .append(getVoteMultiplierComponent(active ? superLevel : standard));

    return builder.build();
  }

  private Component getSuperVoteActiveComponent() {
    return text()
        .append(text("Super Vote", NamedTextColor.LIGHT_PURPLE))
        .appendSpace()
        .append(text("active!", NamedTextColor.GREEN))
        .build();
  }

  private Component getSuperVoteButtonComponent(boolean canUse, int balance, int multiplier) {

    Component activateHover = text()
        .append(text("Click to activate a", NamedTextColor.GRAY))
        .appendSpace()
        .append(text(multiplier + "x", NamedTextColor.LIGHT_PURPLE))
        .appendSpace()
        .append(text("super vote!", NamedTextColor.GRAY))
        .build();

    if (!canUse) {
      activateHover = text()
          .append(text("You don't have any super votes available!", NamedTextColor.GRAY))
          .appendNewline()
          .append(text("Visit ", NamedTextColor.GRAY))
          .append(MessageUtils.getStoreLink())
          .append(text(" to purchase more.", NamedTextColor.GRAY))
          .build();
    }

    TextComponent.Builder builder = text()
        .append(text("[", NamedTextColor.GRAY))
        .append(text(
            "Super Vote",
            canUse ? NamedTextColor.LIGHT_PURPLE : NamedTextColor.DARK_GRAY,
            TextDecoration.BOLD))
        .append(text("]", NamedTextColor.GRAY))
        .hoverEvent(HoverEvent.showText(activateHover));

    if (canUse) {
      builder.clickEvent(ClickEvent.runCommand("/supervote"));
    } else {
      builder.clickEvent(ClickEvent.openUrl(Community.get().getServerConfig().getStoreLink()));
    }

    return builder.build();
  }

  private Component getVoteMultiplierComponent(int multiplier) {

    TextComponent.Builder stdHoverBuilder = text();

    for (Rank rank : Community.get().getServerConfig().getRanksConfig().getRanks()) {
      Component rankComponent = text()
          .append(text(rank.getPrefix() + " " + rank.getName(), rank.getTextColor()))
          .append(text(" has a ", NamedTextColor.GRAY))
          .append(text(rank.getVoteMultiplier() + "x", rank.getTextColor(), TextDecoration.BOLD))
          .append(text(" multiplier", NamedTextColor.GRAY))
          .build();

      stdHoverBuilder.append(rankComponent).appendNewline();
    }

    int value = ((RequestConfig) manager.getConfig()).getSuperVoteMultiplier();
    Component supervote = text()
        .append(MessageUtils.VOTE)
        .append(text(" Super Vote", NamedTextColor.LIGHT_PURPLE))
        .append(text(" boosts your vote by ", NamedTextColor.GRAY))
        .append(text(value, NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
        .build();

    Component shop = text()
        .append(text("Visit ", NamedTextColor.GRAY))
        .append(MessageUtils.getStoreLink())
        .append(text(" to learn more", NamedTextColor.GRAY))
        .build();

    Component stdHover = stdHoverBuilder
        .append(supervote)
        .appendNewline()
        .appendNewline()
        .append(shop)
        .build();

    return text()
        .append(text("Vote Multiplier", NamedTextColor.DARK_AQUA))
        .append(text(":", NamedTextColor.GRAY))
        .appendSpace()
        .append(text(multiplier + "x", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
        .hoverEvent(HoverEvent.showText(stdHover))
        .clickEvent(ClickEvent.openUrl(Community.get().getServerConfig().getStoreLink()))
        .build();
  }
}

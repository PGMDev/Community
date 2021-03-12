package dev.pgm.community.nick.providers;

import static net.kyori.adventure.text.Component.text;

import dev.pgm.community.nick.feature.NickFeature;
import dev.pgm.community.utils.PGMUtils;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.nick.NickProvider;

public class PGMNickProvider implements NickProvider {

  private boolean hotbarColor = false;

  private NickFeature nick;

  private Future<?> hotbarTask;

  public PGMNickProvider(NickFeature nick) {
    this.nick = nick;
    hotbarTask =
        PGM.get().getExecutor().scheduleAtFixedRate(this::updateHotbars, 0, 1, TimeUnit.SECONDS);
  }

  @Override
  public Optional<String> getNick(@Nullable UUID playerId) {
    return Optional.ofNullable(nick.getOnlineNick(playerId));
  }

  public void cancelTask() {
    hotbarTask.cancel(true);
  }

  private void updateHotbars() {
    Match match = PGMUtils.getMatch();
    if (match != null) {
      List<MatchPlayer> nicked =
          match.getPlayers().stream()
              .filter(p -> nick.isNicked(p.getId()))
              .collect(Collectors.toList());
      nicked.forEach(mp -> sendHotbarNicked(mp, hotbarColor));
    }
    hotbarColor = !hotbarColor;
  }

  private void sendHotbarNicked(MatchPlayer player, boolean flashColor) {
    Component warning = text(" \u26a0 ", flashColor ? NamedTextColor.YELLOW : NamedTextColor.GOLD);
    Component nicked =
        text("You are currently disguised", NamedTextColor.DARK_AQUA, TextDecoration.BOLD);
    Component message = text().append(warning).append(nicked).append(warning).build();

    if (player.isObserving()) {
      player.sendActionBar(message);
    }
  }
}

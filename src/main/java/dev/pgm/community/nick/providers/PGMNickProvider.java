package dev.pgm.community.nick.providers;

import static net.kyori.adventure.text.Component.text;

import com.google.common.collect.Maps;
import dev.pgm.community.utils.PGMUtils;
import java.util.List;
import java.util.Map;
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

  private Map<UUID, String> nickedPlayers;

  private boolean hotbarColor = false;

  private Future<?> hotbarTask;

  public PGMNickProvider() {
    this.nickedPlayers = Maps.newHashMap();
    hotbarTask =
        PGM.get().getExecutor().scheduleAtFixedRate(this::updateHotbars, 0, 1, TimeUnit.SECONDS);
  }

  @Override
  public Optional<String> getNick(@Nullable UUID playerId) {
    return Optional.ofNullable(nickedPlayers.get(playerId));
  }

  public void setNick(UUID playerId, String nickName) {
    this.nickedPlayers.put(playerId, nickName);
  }

  public void clearNick(UUID playerId) {
    this.nickedPlayers.remove(playerId);
  }

  public void cancelTask() {
    hotbarTask.cancel(true);
  }

  private void updateHotbars() {
    Match match = PGMUtils.getMatch();
    if (match != null) {
      List<MatchPlayer> nicked =
          match.getPlayers().stream()
              .filter(p -> nickedPlayers.get(p.getId()) != null)
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
    player.sendActionBar(message);
  }
}

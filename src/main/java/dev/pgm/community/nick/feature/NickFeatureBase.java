package dev.pgm.community.nick.feature;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import dev.pgm.community.Community;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.feature.FeatureBase;
import dev.pgm.community.nick.Nick;
import dev.pgm.community.nick.NickConfig;
import dev.pgm.community.nick.commands.NickCommands;
import dev.pgm.community.nick.providers.PGMNickProvider;
import dev.pgm.community.utils.PGMUtils;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import tc.oc.pgm.api.PGM;

public abstract class NickFeatureBase extends FeatureBase implements NickFeature {

  private @Nullable PGMNickProvider pgmNicks;

  private Map<UUID, String> nickedPlayers;

  public NickFeatureBase(Configuration config, Logger logger, String featureName) {
    super(new NickConfig(config), logger, featureName);
    this.nickedPlayers = Maps.newHashMap();

    if (getNickConfig().isEnabled()) {
      enable();
      enablePGMSupport();
    }
  }

  public NickConfig getNickConfig() {
    return (NickConfig) getConfig();
  }

  @Override
  public boolean isNicked(UUID playerId) {
    return nickedPlayers.containsKey(playerId);
  }

  @Override
  public String getOnlineNick(UUID playerId) {
    return nickedPlayers.get(playerId);
  }

  @Override
  public Player getPlayerFromNick(String nickName) {
    Optional<UUID> player =
        nickedPlayers.entrySet().stream()
            .filter((e) -> e.getValue().equalsIgnoreCase(nickName))
            .map(Entry::getKey)
            .findAny();
    return player.isPresent() ? Bukkit.getPlayer(player.get()) : null;
  }

  @Override
  public Set<CommunityCommand> getCommands() {
    return getNickConfig().isEnabled() ? Sets.newHashSet(new NickCommands()) : Sets.newHashSet();
  }

  @EventHandler
  public void onPrelogin(AsyncPlayerPreLoginEvent event) {
    if (!getConfig().isEnabled()) return;
    Nick nick = this.getNick(event.getUniqueId()).join();
    if (nick != null && nick.isEnabled()) {
      nickedPlayers.put(event.getUniqueId(), nick.getNickName());
    } else {
      nickedPlayers.remove(event.getUniqueId());
    }
  }

  private void enablePGMSupport() {
    if (PGMUtils.isPGMEnabled() && getNickConfig().isIntegrationEnabled()) {
      pgmNicks = new PGMNickProvider(this);
      Bukkit.getScheduler()
          .scheduleSyncDelayedTask(
              Community.get(), () -> PGM.get().getNickRegistry().setProvider(pgmNicks));
    }
  }
}

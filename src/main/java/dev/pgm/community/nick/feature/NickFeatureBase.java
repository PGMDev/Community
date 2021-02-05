package dev.pgm.community.nick.feature;

import com.google.common.collect.Sets;
import dev.pgm.community.Community;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.feature.FeatureBase;
import dev.pgm.community.nick.Nick;
import dev.pgm.community.nick.NickConfig;
import dev.pgm.community.nick.commands.NickCommands;
import dev.pgm.community.nick.providers.PGMNickProvider;
import dev.pgm.community.utils.PGMUtils;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import tc.oc.pgm.api.PGM;

public abstract class NickFeatureBase extends FeatureBase implements NickFeature {

  private @Nullable PGMNickProvider pgmNicks;

  private Set<UUID> nickedPlayers;

  public NickFeatureBase(Configuration config, Logger logger, String featureName) {
    super(new NickConfig(config), logger, featureName);
    this.nickedPlayers = Sets.newHashSet();

    if (getNickConfig().isEnabled()) {
      enable();
      enablePGMSupport();
    }
  }

  @Override
  public boolean isNicked(UUID playerId) {
    return nickedPlayers.contains(playerId);
  }

  public NickConfig getNickConfig() {
    return (NickConfig) getConfig();
  }

  private void enablePGMSupport() {
    if (PGMUtils.isPGMEnabled() && getNickConfig().isIntegrationEnabled()) {
      pgmNicks = new PGMNickProvider();
      Bukkit.getScheduler()
          .scheduleSyncDelayedTask(
              Community.get(),
              new Runnable() {
                @Override
                public void run() {
                  logger.info("Successfully hooked into PGM for nick support");
                  PGM.get().getNickRegistry().setProvider(pgmNicks);
                }
              });
    }
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
      pgmNicks.setNick(event.getUniqueId(), nick.getNickName());
      nickedPlayers.add(event.getUniqueId());
    } else {
      pgmNicks.clearNick(event.getUniqueId());
      nickedPlayers.remove(event.getUniqueId());
    }
  }
}

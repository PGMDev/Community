package dev.pgm.community.motd;

import com.google.common.collect.Sets;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.feature.FeatureBase;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.configuration.Configuration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import tc.oc.pgm.util.bukkit.BukkitUtils;

/** MotdFeature - Displays a configurable message at login * */
public class MotdFeature extends FeatureBase {

  public MotdFeature(Configuration config, Logger logger) {
    super(new MotdConfig(config), logger, "MOTD");
    if (getConfig().isEnabled()) {
      enable();
    }
  }

  public MotdConfig getMotdConfig() {
    return (MotdConfig) getConfig();
  }

  @EventHandler(priority = EventPriority.LOW)
  public void onPlayerJoin(PlayerJoinEvent event) {
    if (getMotdConfig().getLines().isEmpty()) return;
    for (String line : getMotdConfig().getLines()) {
      event.getPlayer().sendMessage(BukkitUtils.colorize(line));
    }
  }

  @Override
  public Set<CommunityCommand> getCommands() {
    return Sets.newHashSet();
  }
}

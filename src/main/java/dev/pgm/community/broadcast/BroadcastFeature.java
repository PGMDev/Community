package dev.pgm.community.broadcast;

import static net.kyori.adventure.text.Component.text;
import static tc.oc.pgm.util.bukkit.BukkitUtils.colorize;

import com.google.common.collect.Sets;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.feature.FeatureBase;
import dev.pgm.community.utils.BroadcastUtils;
import dev.pgm.community.utils.Sounds;
import java.util.Set;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.Configuration;

/** BroadcastFeature - Send announcements to inform players about things * */
public class BroadcastFeature extends FeatureBase {

  public BroadcastFeature(Configuration config, Logger logger) {
    super(new BroadcastConfig(config), logger);
  }

  public BroadcastConfig getBroadcastConfig() {
    return (BroadcastConfig) getConfig();
  }

  public void broadcast(String message, boolean title) {
    Component prefix = text(colorize(getBroadcastConfig().getPrefix()));
    Component msg = text(colorize(message));

    if (title) {
      BroadcastUtils.sendGlobalTitle(prefix, msg, getBroadcastConfig().getTitleSeconds());
    } else {
      BroadcastUtils.sendGlobalMessage(text().append(prefix).append(msg).build());
    }

    if (getBroadcastConfig().isSoundEnabled()) {
      BroadcastUtils.playGlobalSound(Sounds.BROADCAST);
    }
  }

  @Override
  public Set<CommunityCommand> getCommands() {
    return getConfig().isEnabled() ? Sets.newHashSet(new BroadcastCommand()) : Sets.newHashSet();
  }
}

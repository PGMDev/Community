package dev.pgm.community.broadcast;

import static net.kyori.adventure.text.Component.text;
import static tc.oc.pgm.util.bukkit.BukkitUtils.colorize;

import dev.pgm.community.Community;
import dev.pgm.community.feature.FeatureBase;
import dev.pgm.community.utils.BroadcastUtils;
import dev.pgm.community.utils.Sounds;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;

/** BroadcastFeature - Send announcements to inform players about things * */
public class BroadcastFeature extends FeatureBase {

  private int announceTask;

  private Instant lastAnnounce;
  private int lastAnnounceIndex;

  public BroadcastFeature(Configuration config, Logger logger) {
    super(new BroadcastConfig(config), logger, "Broadcasts");
    this.lastAnnounce = Instant.now();
    this.lastAnnounceIndex = 0;

    if (getConfig().isEnabled()) {
      enable();
    }
  }

  @Override
  public void enable() {
    super.enable();
    startAnnounceTask();
  }

  @Override
  public void disable() {
    super.disable();
    Bukkit.getScheduler().cancelTask(announceTask);
  }

  private void startAnnounceTask() {
    this.announceTask =
        Bukkit.getScheduler().scheduleSyncRepeatingTask(Community.get(), this::announce, 0L, 20L);
  }

  // TODO: Allow for /toggle tips once shared settings implemented
  private void announce() {
    if (!getBroadcastConfig().isAnnounceEnabled()) return;
    if (getBroadcastConfig().getAnnounceMessages().isEmpty()) return;
    Duration timeSince = Duration.between(lastAnnounce, Instant.now());
    List<String> messages = getBroadcastConfig().getAnnounceMessages();

    if (timeSince.getSeconds() > getBroadcastConfig().getAnnounceDelay()) {
      if (lastAnnounceIndex >= messages.size()) lastAnnounceIndex = 0;
      String msg = messages.get(lastAnnounceIndex);
      Component prefix = text(colorize(getBroadcastConfig().getAnnouncePrefix()));
      Component message = text(colorize(msg));
      BroadcastUtils.sendGlobalMessage(text().append(prefix).append(message).build());
      lastAnnounceIndex++;
      lastAnnounce = Instant.now();
    }
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
}

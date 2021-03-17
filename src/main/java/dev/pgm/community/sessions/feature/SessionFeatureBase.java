package dev.pgm.community.sessions.feature;

import com.google.common.collect.Sets;
import dev.pgm.community.Community;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.feature.FeatureBase;
import dev.pgm.community.sessions.SessionConfig;
import dev.pgm.community.sessions.VanishedSessionListener;
import dev.pgm.community.utils.PGMUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public abstract class SessionFeatureBase extends FeatureBase implements SessionFeature {

  private List<Player>
      vanishedTransitions; // players who have joined and are about to be vanished, or players who
  // have left and are about to be unvanished
  private VanishedSessionListener vanishedSessionListener;

  public SessionFeatureBase(Configuration config, Logger logger, String featureName) {
    super(new SessionConfig(config), logger, featureName);

    if (getConfig().isEnabled()) {
      this.vanishedTransitions = new ArrayList<Player>();
      enable();

      if (PGMUtils.isPGMEnabled()) {
        vanishedSessionListener = new VanishedSessionListener(this);
        Bukkit.getPluginManager().registerEvents(vanishedSessionListener, Community.get());
      }
    }
  }

  @Override
  public void disable() {
    if (vanishedSessionListener != null) HandlerList.unregisterAll(vanishedSessionListener);

    for (Player player : Bukkit.getOnlinePlayers())
      getLatestSession(player.getUniqueId(), false).thenAcceptAsync(this::endSession);
  }

  public SessionConfig getSessionConfig() {
    return (SessionConfig) getConfig();
  }

  @Override
  public Set<CommunityCommand> getCommands() {
    return Sets.newHashSet();
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onJoinLowest(PlayerJoinEvent event) {
    vanishedTransitions.add(event.getPlayer());
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onJoinHighest(PlayerJoinEvent event) {
    vanishedTransitions.remove(event.getPlayer());
    startSession(event.getPlayer());
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onQuitLowest(PlayerQuitEvent event) {
    vanishedTransitions.add(event.getPlayer());
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onQuitHighest(PlayerQuitEvent event) {
    vanishedTransitions.remove(event.getPlayer());
    getLatestSession(event.getPlayer().getUniqueId(), false).thenAcceptAsync(this::endSession);
  }

  @Override
  public boolean isInTransition(Player player) {
    return vanishedTransitions.contains(player);
  }
}

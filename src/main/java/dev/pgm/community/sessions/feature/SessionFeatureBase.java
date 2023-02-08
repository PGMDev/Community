package dev.pgm.community.sessions.feature;

import dev.pgm.community.Community;
import dev.pgm.community.feature.FeatureBase;
import dev.pgm.community.sessions.VanishedSessionListener;
import dev.pgm.community.users.feature.UsersFeature;
import dev.pgm.community.utils.PGMUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import tc.oc.pgm.events.CountdownStartEvent;
import tc.oc.pgm.restart.RestartCountdown;

public abstract class SessionFeatureBase extends FeatureBase implements SessionFeature {

  private List<UUID> joiningPlayers;
  private VanishedSessionListener vanishedSessionListener;

  private boolean serverRestarting;

  public SessionFeatureBase(UsersFeature users, Logger logger, String featureName) {
    super(users.getConfig(), logger, featureName);

    if (getConfig().isEnabled()) {
      this.joiningPlayers = new ArrayList<UUID>();
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

  @EventHandler(priority = EventPriority.LOWEST)
  public void onJoinLowest(PlayerJoinEvent event) {
    joiningPlayers.add(event.getPlayer().getUniqueId());
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onJoinHighest(PlayerJoinEvent event) {
    if (!serverRestarting) startSession(event.getPlayer());
    joiningPlayers.remove(event.getPlayer().getUniqueId());
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onQuitHighest(PlayerQuitEvent event) {
    if (!serverRestarting)
      getLatestSession(event.getPlayer().getUniqueId(), false).thenAcceptAsync(this::endSession);
  }

  @Override
  public boolean isPlayerJoining(Player player) {
    return joiningPlayers.contains(player.getUniqueId());
  }

  @EventHandler
  public void onServerRestart(CountdownStartEvent event) {
    if (!(event.getCountdown() instanceof RestartCountdown)) return;

    // When the server restarts, we may have more than 80 queries being sent to the server.
    // As an optimisation, let's end all our session on this server now with one query.
    // We also won't create any new sessions now, since they'll be moved to the new server
    // in a few seconds anyway.
    serverRestarting = true;
    endOngoingSessions();
  }
}

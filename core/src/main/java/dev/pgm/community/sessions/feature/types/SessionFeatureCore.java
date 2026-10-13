package dev.pgm.community.sessions.feature.types;

import dev.pgm.community.Community;
import dev.pgm.community.feature.FeatureBase;
import dev.pgm.community.sessions.Session;
import dev.pgm.community.sessions.SessionQuery;
import dev.pgm.community.sessions.VanishedSessionListener;
import dev.pgm.community.sessions.feature.SessionFeature;
import dev.pgm.community.sessions.store.SessionStore;
import dev.pgm.community.users.feature.UsersFeature;
import dev.pgm.community.utils.PGMUtils;
import dev.pgm.community.utils.VisibilityUtils;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import tc.oc.pgm.events.CountdownCancelEvent;
import tc.oc.pgm.events.CountdownStartEvent;
import tc.oc.pgm.restart.RestartCountdown;

public class SessionFeatureCore extends FeatureBase implements SessionFeature {

  private final SessionStore store;
  private final Map<UUID, Session> activeSessions;
  private VanishedSessionListener vanishedSessionListener;
  private boolean serverRestarting;

  public SessionFeatureCore(UsersFeature users, Logger logger, SessionStore store) {
    super(users.getConfig(), logger, "Sessions");
    this.store = store;
    this.activeSessions = new ConcurrentHashMap<>();

    if (getConfig().isEnabled()) {
      long cleanupStarted = System.nanoTime();
      logger.info("Scheduling orphaned session cleanup");
      store
          .closeOrphanedSessions(Community.get().getServerId(), Instant.now())
          .whenComplete((ignored, exception) -> {
            long elapsedMillis = (System.nanoTime() - cleanupStarted) / 1_000_000;
            if (exception != null) {
              logger.warning("Failed to close orphaned sessions after " + elapsedMillis + " ms: "
                  + exception.getMessage());
            } else {
              logger.info("Orphaned session cleanup completed in " + elapsedMillis + " ms");
            }
          });

      enable();

      if (PGMUtils.isPGMEnabled()) {
        vanishedSessionListener = new VanishedSessionListener(this);
        Bukkit.getPluginManager().registerEvents(vanishedSessionListener, Community.get());
      }
    }
  }

  @Override
  public void disable() {
    super.disable();
    if (vanishedSessionListener != null) HandlerList.unregisterAll(vanishedSessionListener);
    endOngoingSessionsSync();
  }

  @Override
  public CompletableFuture<Session> getLatestSession(UUID playerId, boolean ignoreDisguised) {
    return store.query(new SessionQuery(playerId, ignoreDisguised));
  }

  @Override
  public Session getActiveSession(UUID playerId) {
    return activeSessions.get(playerId);
  }

  @Override
  public void startSession(Player player) {
    startSession(player, VisibilityUtils.isDisguised(player));
  }

  @Override
  public void startSession(Player player, boolean disguised) {
    Session session = new Session(player.getUniqueId(), disguised);
    activeSessions.put(player.getUniqueId(), session);
    store.save(session);
  }

  @Override
  public void endSession(Session session) {
    activeSessions.remove(session.getPlayerId(), session);
    session.setEndDate(Instant.now());
    store.updateSessionEndTime(session);
  }

  @Override
  public void endOngoingSessions() {
    Instant now = Instant.now();
    for (Session session : activeSessions.values()) {
      session.setEndDate(now);
      store.updateSessionEndTime(session);
    }
    activeSessions.clear();
  }

  @Override
  public void endOngoingSessionsSync() {
    Instant now = Instant.now();
    for (Session session : activeSessions.values()) {
      session.setEndDate(now);
      store.updateSessionEndTimeSync(session);
    }
    activeSessions.clear();
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onJoinHighest(PlayerJoinEvent event) {
    if (!serverRestarting) startSession(event.getPlayer());
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onQuitHighest(PlayerQuitEvent event) {
    Session session = activeSessions.remove(event.getPlayer().getUniqueId());
    if (session != null) {
      endSession(session);
    }
  }

  @Override
  public boolean isPlayerJoining(Player player) {
    return !activeSessions.containsKey(player.getUniqueId());
  }

  @EventHandler
  public void onServerRestart(CountdownStartEvent event) {
    if (!(event.getCountdown() instanceof RestartCountdown)) return;

    // When restarting, end sessions early to avoid flooding queries.
    serverRestarting = true;
    endOngoingSessions();
  }

  @EventHandler
  public void onServerRestartCancel(CountdownCancelEvent event) {
    if (!(event.getCountdown() instanceof RestartCountdown) || !serverRestarting) return;

    // Sessions were ended when the countdown started, so resume tracking online players
    serverRestarting = false;
    for (Player player : Bukkit.getOnlinePlayers()) {
      if (!activeSessions.containsKey(player.getUniqueId())) startSession(player);
    }
  }
}

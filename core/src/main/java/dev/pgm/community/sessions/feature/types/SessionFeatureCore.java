package dev.pgm.community.sessions.feature.types;

import dev.pgm.community.Community;
import dev.pgm.community.feature.FeatureBase;
import dev.pgm.community.nick.feature.NickFeature;
import dev.pgm.community.sessions.DisguiseSessionListener;
import dev.pgm.community.sessions.Session;
import dev.pgm.community.sessions.SessionQuery;
import dev.pgm.community.sessions.feature.SessionFeature;
import dev.pgm.community.sessions.store.SessionStore;
import dev.pgm.community.users.feature.UsersFeature;
import dev.pgm.community.utils.PGMUtils;
import dev.pgm.community.utils.VisibilityUtils;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class SessionFeatureCore extends FeatureBase implements SessionFeature {

  private static final long DISABLE_TIMEOUT_SECONDS = 30;

  private final Supplier<NickFeature> nicks;
  private final SessionStore store;
  private final Map<UUID, Session> activeSessions;
  private DisguiseSessionListener disguiseSessionListener;
  private boolean started;

  public SessionFeatureCore(
      UsersFeature users, Supplier<NickFeature> nicks, Logger logger, SessionStore store) {
    super(users.getConfig(), logger, "Sessions");
    this.nicks = nicks;
    this.store = store;
    this.activeSessions = new ConcurrentHashMap<>();

    if (getConfig().isEnabled()) {
      String serverId = Community.get().getServerId();
      if (isDefaultServerId(serverId)) {
        logger.warning("Server ID is the default \"" + serverId
            + "\". Instances sharing an ID will close each other's sessions on startup.");
      }

      // A count after a clean shutdown suggests another instance shares this server ID
      long cleanupStarted = System.nanoTime();
      store.closeOrphanedSessions(serverId, Instant.now()).whenComplete((closed, error) -> {
        long elapsedMillis = (System.nanoTime() - cleanupStarted) / 1_000_000;
        if (error != null) {
          logger.warning(
              "Failed to close orphaned sessions after " + elapsedMillis + " ms: " + error);
        } else if (closed > 0) {
          logger.info("Closed " + closed + " orphaned sessions for server " + serverId + " in "
              + elapsedMillis + " ms");
        }
      });

      enable();
      started = true;

      if (PGMUtils.isPGMEnabled()) {
        disguiseSessionListener = new DisguiseSessionListener(this);
        Bukkit.getPluginManager().registerEvents(disguiseSessionListener, Community.get());
      }
    }
  }

  @Override
  public void disable() {
    if (!started) return;
    started = false;
    super.disable();
    if (disguiseSessionListener != null) HandlerList.unregisterAll(disguiseSessionListener);
    endAllSessions();
  }

  @Override
  public CompletableFuture<Session> getLatestSession(UUID playerId, boolean ignoreDisguised) {
    Session active = activeSessions.get(playerId);
    if (active != null && !(ignoreDisguised && active.isDisguised())) {
      return CompletableFuture.completedFuture(active);
    }
    return store.query(new SessionQuery(playerId, ignoreDisguised));
  }

  @Override
  public Session getActiveSession(UUID playerId) {
    return activeSessions.get(playerId);
  }

  private boolean isDisguised(Player player) {
    return VisibilityUtils.isDisguised(player) || nicks.get().isNickPending(player.getUniqueId());
  }

  private void startSession(Player player, Instant startTime) {
    Session session = new Session(player.getUniqueId(), isDisguised(player), startTime);
    activeSessions.put(player.getUniqueId(), session);
    store.save(session);
  }

  @Override
  public void updateDisguise(Player player) {
    Session previous = getActiveSession(player.getUniqueId());
    if (previous == null || previous.isDisguised() == isDisguised(player)) return;

    Instant now = Instant.now();
    store.updateSessionEndTime(player.getUniqueId(), previous.getSessionId(), now);
    startSession(player, now);
    previous.setEndDate(now);
  }

  private void endSession(Session session, Instant endTime) {
    session.setEndDate(endTime);
    store.updateSessionEndTime(session.getPlayerId(), session.getSessionId(), endTime);
    activeSessions.remove(session.getPlayerId(), session);
  }

  private void endAllSessions() {
    Instant now = Instant.now();
    for (Session session : activeSessions.values()) {
      endSession(session, now);
    }

    try {
      store.shutdownFlush().orTimeout(DISABLE_TIMEOUT_SECONDS, TimeUnit.SECONDS).join();
    } catch (CompletionException e) {
      logger.warning("Timed out waiting for session writes. Sessions not yet ended will be closed"
          + " on next startup as zero-length, losing their playtime: " + e.getMessage());
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onJoinHighest(PlayerJoinEvent event) {
    startSession(event.getPlayer(), Instant.now());
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onQuit(PlayerQuitEvent event) {
    Session session = getActiveSession(event.getPlayer().getUniqueId());
    if (session != null) {
      endSession(session, Instant.now());
    }
  }

  private static boolean isDefaultServerId(String serverId) {
    Configuration defaults = Community.get().getConfig().getDefaults();
    return defaults != null && serverId.equals(defaults.getString("general.server-id"));
  }
}

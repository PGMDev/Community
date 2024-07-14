package dev.pgm.community.sessions.feature.types;

import dev.pgm.community.sessions.Session;
import dev.pgm.community.sessions.SessionQuery;
import dev.pgm.community.sessions.feature.SessionFeatureBase;
import dev.pgm.community.sessions.services.SQLSessionService;
import dev.pgm.community.users.feature.UsersFeature;
import dev.pgm.community.utils.VisibilityUtils;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.bukkit.entity.Player;

public class SQLSessionFeature extends SessionFeatureBase {

  private final SQLSessionService service;

  public SQLSessionFeature(UsersFeature users, Logger logger) {
    super(users, logger, "Sessions (SQL)");
    this.service = new SQLSessionService();
  }

  @Override
  public CompletableFuture<Session> getLatestSession(UUID playerId, boolean ignoreDisguised) {
    return service.query(new SessionQuery(playerId, ignoreDisguised));
  }

  @Override
  public Session startSession(Player player) {
    Session session = new Session(player.getUniqueId(), VisibilityUtils.isDisguised(player));
    service.save(session);

    return session;
  }

  @Override
  public void endSession(Session session) {
    session.setEndDate(Instant.now());
    service.updateSessionEndTime(session);
  }

  @Override
  public void endOngoingSessions() {
    service.endOngoingSessions();
  }
}

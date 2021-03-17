package dev.pgm.community.sessions.feature.types;

import dev.pgm.community.database.DatabaseConnection;
import dev.pgm.community.sessions.Session;
import dev.pgm.community.sessions.SessionQuery;
import dev.pgm.community.sessions.feature.SessionFeatureBase;
import dev.pgm.community.sessions.services.SQLSessionService;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;

public class SQLSessionFeature extends SessionFeatureBase {

  private final SQLSessionService service;

  public SQLSessionFeature(Configuration config, Logger logger, DatabaseConnection connection) {
    super(config, logger, "Sessions (SQL)");
    this.service = new SQLSessionService(connection);
  }

  @Override
  public CompletableFuture<Session> getLatestSession(UUID playerId, boolean ignoreDisguised) {
    return service.query(new SessionQuery(playerId, ignoreDisguised));
  }

  @Override
  public Session startSession(Player player) {
    Session session = new Session(player.getUniqueId(), player.hasMetadata("isVanished"));
    service.save(session);

    return session;
  }

  @Override
  public void endSession(Session session) {
    session.setEndDate(Instant.now());
    service.updateSessionEndTime(session);
  }
}

package dev.pgm.community.sessions.store;

import dev.pgm.community.sessions.Session;
import dev.pgm.community.sessions.SessionQuery;
import dev.pgm.community.sessions.services.SQLSessionService;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SQLSessionStore implements SessionStore {

  private final SQLSessionService service;

  public SQLSessionStore() {
    this.service = new SQLSessionService();
  }

  @Override
  public void save(Session session) {
    service.save(session);
  }

  @Override
  public CompletableFuture<Session> query(SessionQuery target) {
    return service.query(target);
  }

  @Override
  public void updateSessionEndTime(UUID playerId, UUID sessionId, Instant endTime) {
    service.updateSessionEndTime(playerId, sessionId, endTime);
  }

  @Override
  public CompletableFuture<Integer> closeOrphanedSessions(String server, Instant bootTime) {
    return service.closeOrphanedSessions(server, bootTime);
  }

  @Override
  public CompletableFuture<Void> shutdownFlush() {
    return service.shutdownFlush();
  }
}

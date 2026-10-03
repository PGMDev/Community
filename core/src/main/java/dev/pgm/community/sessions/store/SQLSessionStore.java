package dev.pgm.community.sessions.store;

import dev.pgm.community.sessions.Session;
import dev.pgm.community.sessions.SessionQuery;
import dev.pgm.community.sessions.services.SQLSessionService;
import java.time.Instant;
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
  public void updateSessionEndTime(Session session) {
    service.updateSessionEndTime(session);
  }

  @Override
  public void updateSessionEndTimeSync(Session session) {
    service.updateSessionEndTimeSync(session);
  }

  @Override
  public CompletableFuture<Void> closeOrphanedSessions(String server, Instant endTime) {
    return service.closeOrphanedSessions(server, endTime);
  }
}

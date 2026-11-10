package dev.pgm.community.sessions.store;

import dev.pgm.community.sessions.Session;
import dev.pgm.community.sessions.SessionQuery;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface SessionStore {

  void save(Session session);

  CompletableFuture<Session> query(SessionQuery target);

  void updateSessionEndTime(UUID playerId, UUID sessionId, Instant endTime);

  CompletableFuture<Integer> closeOrphanedSessions(String server, Instant bootTime);

  CompletableFuture<Void> shutdownFlush();
}

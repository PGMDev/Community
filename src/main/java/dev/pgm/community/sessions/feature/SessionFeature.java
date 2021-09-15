package dev.pgm.community.sessions.feature;

import dev.pgm.community.feature.Feature;
import dev.pgm.community.sessions.Session;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.entity.Player;

public interface SessionFeature extends Feature {

  CompletableFuture<Session> getLatestSession(UUID playerId, boolean ignoreDisguised);

  Session startSession(Player player);

  void endSession(Session session);

  boolean isPlayerJoining(Player player);
}

package dev.pgm.community.moderation.store;

import dev.pgm.community.moderation.punishments.Punishment;
import dev.pgm.community.moderation.punishments.PunishmentType;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.Nullable;

public interface ModerationStore {

  void save(Punishment punishment);

  CompletableFuture<List<Punishment>> queryList(String target);

  CompletableFuture<List<Punishment>> queryActiveForLogin(String target);

  CompletableFuture<Boolean> pardon(UUID id, @Nullable UUID issuer);

  CompletableFuture<Boolean> deactivate(UUID id, PunishmentType punishmentType);

  CompletableFuture<Boolean> unmute(UUID id, @Nullable UUID issuer);

  CompletableFuture<Boolean> isBanned(String id);

  CompletableFuture<Optional<Punishment>> isMuted(UUID target);

  CompletableFuture<Optional<Punishment>> getActiveBan(String id);

  CompletableFuture<List<Punishment>> getRecentPunishments(Duration period);

  void invalidate(UUID playerId);

  CompletableFuture<Integer> count();
}

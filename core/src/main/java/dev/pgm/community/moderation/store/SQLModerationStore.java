package dev.pgm.community.moderation.store;

import dev.pgm.community.moderation.ModerationConfig;
import dev.pgm.community.moderation.punishments.Punishment;
import dev.pgm.community.moderation.punishments.PunishmentType;
import dev.pgm.community.moderation.services.SQLModerationService;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.Nullable;

public class SQLModerationStore implements ModerationStore {

  private final SQLModerationService service;

  public SQLModerationStore(ModerationConfig config) {
    this.service = new SQLModerationService(config);
  }

  @Override
  public void save(Punishment punishment) {
    service.save(punishment);
  }

  @Override
  public CompletableFuture<List<Punishment>> queryList(String target) {
    return service.queryList(target);
  }

  @Override
  public CompletableFuture<List<Punishment>> queryActiveForLogin(String target) {
    return service.queryActiveForLogin(target);
  }

  @Override
  public CompletableFuture<Boolean> pardon(UUID id, @Nullable UUID issuer) {
    return service.pardon(id, issuer);
  }

  @Override
  public CompletableFuture<Boolean> deactivate(UUID id, PunishmentType punishmentType) {
    return service.deactivate(id, punishmentType);
  }

  @Override
  public CompletableFuture<Boolean> unmute(UUID id, @Nullable UUID issuer) {
    return service.unmute(id, issuer);
  }

  @Override
  public CompletableFuture<Boolean> isBanned(String id) {
    return service.isBanned(id);
  }

  @Override
  public CompletableFuture<Optional<Punishment>> isMuted(UUID target) {
    return service.isMuted(target);
  }

  @Override
  public CompletableFuture<Optional<Punishment>> getActiveBan(String id) {
    return service.getActiveBan(id);
  }

  @Override
  public CompletableFuture<List<Punishment>> getRecentPunishments(Duration period) {
    return service.getRecentPunishments(period);
  }

  @Override
  public void invalidate(UUID playerId) {
    service.invalidate(playerId);
  }

  @Override
  public CompletableFuture<Integer> count() {
    return service.count();
  }
}

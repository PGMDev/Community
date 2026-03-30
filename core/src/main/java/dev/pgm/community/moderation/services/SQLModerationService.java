package dev.pgm.community.moderation.services;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import dev.pgm.community.database.DatabaseExecutor;
import dev.pgm.community.feature.SQLFeatureBase;
import dev.pgm.community.moderation.ModerationConfig;
import dev.pgm.community.moderation.punishments.Punishment;
import dev.pgm.community.moderation.punishments.PunishmentType;
import dev.pgm.community.moderation.punishments.types.ExpirablePunishment;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.Nullable;

public class SQLModerationService extends SQLFeatureBase<Punishment, String>
    implements ModerationQuery {

  private static final String CONSOLE_DB_NAME =
      "console"; // Used in issuer field when punishment issued by console

  private static final int RECENT_LIMIT = 50;

  private final LoadingCache<UUID, PlayerPunishments> punishmentCache;

  private final ModerationConfig config;

  public SQLModerationService(ModerationConfig config) {
    super(TABLE_NAME, TABLE_FIELDS);
    this.config = config;
    this.punishmentCache =
        CacheBuilder.newBuilder().build(CacheLoader.from(PlayerPunishments::new));
  }

  @Override
  public void save(Punishment punishment) {
    PlayerPunishments punishments = punishmentCache.getUnchecked(punishment.getTargetId());
    if (punishments.isLoaded()) {
      punishments.getPunishments().add(punishment);
    }

    DatabaseExecutor.executeUpdateAsync(
        INSERT_PUNISHMENT_QUERY,
        punishment.getId().toString(),
        punishment.getTargetId().toString(),
        convertIssuer(punishment.getIssuerId()),
        punishment.getReason(),
        punishment.getType().toString(),
        punishment.getTimeIssued().toEpochMilli(),
        getExpiration(punishment),
        punishment.isActive(),
        punishment.getTimeIssued().toEpochMilli(),
        convertIssuer(punishment.getIssuerId()),
        config.getService());
  }

  @Override
  public CompletableFuture<List<Punishment>> queryList(String target) {
    UUID playerId = UUID.fromString(target);
    PlayerPunishments punishments = punishmentCache.getUnchecked(playerId);

    if (punishments.isLoaded()) {
      return CompletableFuture.completedFuture(punishments.getPunishments());
    } else {
      return DatabaseExecutor.queryAsync(
              SELECT_PUNISHMENTS_QUERY, row -> mapPunishment(row, playerId), playerId.toString())
          .thenApplyAsync(results -> {
            if (results != null && !results.isEmpty()) {
              punishments.getPunishments().addAll(results);
            }

            punishments.setLoaded(true);

            return punishments.getPunishments();
          });
    }
  }

  public CompletableFuture<List<Punishment>> queryActiveForLogin(String target) {
    UUID playerId = UUID.fromString(target);
    return DatabaseExecutor.queryAsync(
            SELECT_ACTIVE_PUNISHMENTS_QUERY,
            row -> mapActivePunishment(row, playerId),
            playerId.toString(),
            true)
        .thenApplyAsync(results -> {
          if (results == null) {
            return Lists.newArrayList();
          }
          return results;
        });
  }

  @Override // TODO: fetch single punishment
  public CompletableFuture<Punishment> query(String target) {
    return CompletableFuture.completedFuture(null);
  } // Noop

  /**
   * Note: by default all punishments have an expires that is the exact time issued,
   * ExpirablePunishments can be checked if the expires field is != time field.
   */
  private long getExpiration(Punishment punishment) {
    long expires = punishment.getTimeIssued().toEpochMilli();

    if (punishment instanceof ExpirablePunishment) {
      Duration time = punishment.getDuration();
      expires = punishment.getTimeIssued().plusMillis(time.toMillis()).toEpochMilli();
    }

    return expires;
  }

  private boolean isConsole(String data) {
    return data.equalsIgnoreCase(CONSOLE_DB_NAME);
  }

  private UUID parseIssuer(String issuer) {
    if (isConsole(issuer)) return null;
    return UUID.fromString(issuer);
  }

  private String convertIssuer(@Nullable UUID issuer) {
    return issuer != null ? issuer.toString() : CONSOLE_DB_NAME;
  }

  public CompletableFuture<Boolean> pardon(UUID id, @Nullable UUID issuer) {
    punishmentCache.invalidate(id);
    return DatabaseExecutor.executeUpdateAsync(
            PARDON_QUERY + MULTI_PARDON_TYPE,
            false,
            Instant.now().toEpochMilli(),
            convertIssuer(issuer),
            true,
            id.toString(),
            PunishmentType.BAN.toString(),
            PunishmentType.TEMP_BAN.toString(),
            PunishmentType.NAME_BAN.toString())
        .thenApplyAsync(result -> result != 0);
  }

  public CompletableFuture<Boolean> deactivate(UUID id, PunishmentType punishmentType) {
    punishmentCache.invalidate(id);
    return DatabaseExecutor.executeUpdateAsync(
            DEACTIVATE_QUERY + SINGLE_PARDON_TYPE,
            false,
            true,
            id.toString(),
            punishmentType.toString())
        .thenApplyAsync(result -> result != 0);
  }

  public CompletableFuture<Boolean> unmute(UUID id, @Nullable UUID issuer) {
    punishmentCache.invalidate(id);

    return DatabaseExecutor.executeUpdateAsync(
            PARDON_QUERY + SINGLE_PARDON_TYPE,
            false,
            Instant.now().toEpochMilli(),
            convertIssuer(issuer),
            true,
            id.toString(),
            PunishmentType.MUTE.toString())
        .thenApplyAsync(result -> result != 0);
  }

  public CompletableFuture<Boolean> isBanned(String id) {
    return queryList(id).thenApplyAsync(punishments -> {
      boolean banned = false;
      for (Punishment p : punishments) {
        if (p.getType().isLoginPrevented() && p.isActive()) {
          banned = true;
          break;
        }
      }
      return banned;
    });
  }

  public CompletableFuture<Optional<Punishment>> isMuted(UUID target) {
    return queryList(target.toString()).thenApplyAsync(punishments -> {
      if (punishments.isEmpty()) return Optional.empty();
      return punishments.stream()
          .filter(p -> p.getType() == PunishmentType.MUTE && p.isActive())
          .findFirst();
    });
  }

  public CompletableFuture<Optional<Punishment>> getActiveBan(String id) {
    return queryList(id).thenApplyAsync(punishments -> {
      for (Punishment p : punishments) {
        if (p.getType().isLoginPrevented() && p.isActive()) {
          return Optional.of(p);
        }
      }
      return Optional.empty();
    });
  }

  public CompletableFuture<List<Punishment>> getRecentPunishments(Duration period) {
    return DatabaseExecutor.queryAsync(
            SELECT_RECENT_QUERY,
            this::mapRecentPunishment,
            Instant.now().toEpochMilli() - period.toMillis(),
            RECENT_LIMIT)
        .thenApplyAsync(results -> {
          List<Punishment> punishments = Lists.newArrayList();
          if (results != null && !results.isEmpty()) {
            punishments.addAll(results);
          }
          return punishments;
        });
  }

  public void invalidate(UUID playerId) {
    if (punishmentCache.getIfPresent(playerId) != null) {
      punishmentCache.invalidate(playerId);
    }
  }

  private static class PlayerPunishments {

    private final UUID playerId;
    private final List<Punishment> punishments;
    private boolean loaded;

    public PlayerPunishments(UUID playerId) {
      this.playerId = playerId;
      this.punishments = Lists.newArrayList();
      this.loaded = false;
    }

    public UUID getPlayerId() {
      return playerId;
    }

    public List<Punishment> getPunishments() {
      return punishments;
    }

    public boolean isLoaded() {
      return loaded;
    }

    public void setLoaded(boolean loaded) {
      this.loaded = loaded;
    }
  }

  private Punishment mapPunishment(java.sql.ResultSet row, UUID playerId)
      throws java.sql.SQLException {
    String id = row.getString("id");
    String issuer = row.getString("issuer");
    String reason = row.getString("reason");
    String type = row.getString("type");
    long time = row.getLong("time");
    long expires = row.getLong("expires");
    Duration length = Duration.between(Instant.ofEpochMilli(time), Instant.ofEpochMilli(expires));
    boolean active = row.getBoolean("active");
    long lastUpdateTime = row.getLong("last_updated");
    String lastUpdateBy = row.getString("updated_by");
    String service = row.getString("service");

    return Punishment.of(
        UUID.fromString(id),
        playerId,
        parseIssuer(issuer),
        reason,
        time,
        length,
        PunishmentType.valueOf(type.toUpperCase()),
        active,
        lastUpdateTime,
        parseIssuer(lastUpdateBy),
        service);
  }

  private Punishment mapActivePunishment(java.sql.ResultSet row, UUID playerId)
      throws java.sql.SQLException {
    String id = row.getString("id");
    String issuer = row.getString("issuer");
    String reason = row.getString("reason");
    String type = row.getString("type");
    long time = row.getLong("time");
    long expires = row.getLong("expires");
    Duration length = Duration.between(Instant.ofEpochMilli(time), Instant.ofEpochMilli(expires));
    boolean active = row.getBoolean("active");
    String service = row.getString("service");

    return Punishment.of(
        UUID.fromString(id),
        playerId,
        parseIssuer(issuer),
        reason,
        time,
        length,
        PunishmentType.valueOf(type.toUpperCase()),
        active,
        time,
        null,
        service);
  }

  private Punishment mapRecentPunishment(java.sql.ResultSet row) throws java.sql.SQLException {
    String id = row.getString("id");
    String target = row.getString("punished");
    String issuer = row.getString("issuer");
    String reason = row.getString("reason");
    String type = row.getString("type");
    long time = row.getLong("time");
    long expires = row.getLong("expires");
    Duration length = Duration.between(Instant.ofEpochMilli(time), Instant.ofEpochMilli(expires));
    boolean active = row.getBoolean("active");
    long lastUpdateTime = row.getLong("last_updated");
    String lastUpdateBy = row.getString("updated_by");
    String service = row.getString("service");
    return Punishment.of(
        UUID.fromString(id),
        UUID.fromString(target),
        parseIssuer(issuer),
        reason,
        time,
        length,
        PunishmentType.valueOf(type.toUpperCase()),
        active,
        lastUpdateTime,
        parseIssuer(lastUpdateBy),
        service);
  }
}

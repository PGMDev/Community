package dev.pgm.community.moderation.services;

import co.aikar.idb.DB;
import co.aikar.idb.DbRow;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
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

public class SQLModerationService extends SQLFeatureBase<Punishment, String>
    implements ModerationQuery {

  private static final String CONSOLE_DB_NAME =
      "console"; // Used in issuer field when punishment issued by console

  private static final int RECENT_LIMIT = 50;

  private LoadingCache<UUID, PlayerPunishments> punishmentCache;

  private final ModerationConfig config;

  public SQLModerationService(ModerationConfig config) {
    super(TABLE_NAME, TABLE_FIELDS);
    this.config = config;
    this.punishmentCache =
        CacheBuilder.newBuilder()
            .build(
                new CacheLoader<UUID, PlayerPunishments>() {
                  @Override
                  public PlayerPunishments load(UUID key) throws Exception {
                    return new PlayerPunishments(key);
                  }
                });
  }

  @Override
  public void save(Punishment punishment) {
    PlayerPunishments punishments = punishmentCache.getUnchecked(punishment.getTargetId());
    if (punishments.isLoaded()) {
      punishments.getPunishments().add(punishment);
    }

    DB.executeUpdateAsync(
        INSERT_PUNISHMENT_QUERY,
        UUID.randomUUID().toString(),
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
      return DB.getResultsAsync(SELECT_PUNISHMENTS_QUERY, playerId.toString())
          .thenApplyAsync(
              results -> {
                if (results != null && !results.isEmpty()) {
                  for (DbRow row : results) {
                    String id = row.getString("id");
                    String issuer = row.getString("issuer");
                    String reason = row.getString("reason");
                    String type = row.getString("type");
                    long time = Long.parseLong(row.getString("time"));
                    long expires = Long.parseLong(row.getString("expires"));
                    Instant timeIssued = Instant.ofEpochMilli(time);
                    Duration length =
                        Duration.between(Instant.ofEpochMilli(time), Instant.ofEpochMilli(expires));
                    boolean active = row.get("active");
                    long lastUpdateTime = Long.parseLong(row.getString("last_updated"));
                    Instant lastUpdate = Instant.ofEpochMilli(lastUpdateTime);
                    String lastUpdateBy = row.getString("updated_by");
                    String service = row.getString("service");

                    punishments
                        .getPunishments()
                        .add(
                            Punishment.of(
                                UUID.fromString(id),
                                playerId,
                                parseIssuer(issuer),
                                reason,
                                timeIssued,
                                length,
                                PunishmentType.valueOf(type.toUpperCase()),
                                active,
                                lastUpdate,
                                parseIssuer(lastUpdateBy),
                                service));
                  }
                }

                punishments.setLoaded(true);

                return punishments.getPunishments();
              });
    }
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
      Duration time = ((ExpirablePunishment) punishment).getDuration();
      expires = punishment.getTimeIssued().plusMillis(time.toMillis()).toEpochMilli();
    }

    return expires;
  }

  private boolean isConsole(String data) {
    return data.equalsIgnoreCase(CONSOLE_DB_NAME);
  }

  private Optional<UUID> parseIssuer(String issuer) {
    if (isConsole(issuer)) return Optional.empty();
    return Optional.of(UUID.fromString(issuer));
  }

  private String convertIssuer(Optional<UUID> issuer) {
    return issuer.isPresent() ? issuer.get().toString() : CONSOLE_DB_NAME;
  }

  public CompletableFuture<Boolean> pardon(UUID id, Optional<UUID> issuer) {
    punishmentCache.invalidate(id);
    return DB.executeUpdateAsync(
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
    return DB.executeUpdateAsync(
            DEACTIVATE_QUERY + SINGLE_PARDON_TYPE,
            false,
            true,
            id.toString(),
            punishmentType.toString())
        .thenApplyAsync(result -> result != 0);
  }

  public CompletableFuture<Boolean> unmute(UUID id, Optional<UUID> issuer) {
    punishmentCache.invalidate(id);

    return DB.executeUpdateAsync(
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
    return queryList(id)
        .thenApplyAsync(
            punishments -> {
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
    return queryList(target.toString())
        .thenApplyAsync(
            punishments -> {
              if (punishments.isEmpty()) return Optional.empty();
              return punishments.stream()
                  .filter(p -> p.getType() == PunishmentType.MUTE && p.isActive())
                  .findFirst();
            });
  }

  public CompletableFuture<Optional<Punishment>> getActiveBan(String id) {
    return queryList(id)
        .thenApplyAsync(
            punishments -> {
              for (Punishment p : punishments) {
                if (p.getType().isLoginPrevented() && p.isActive()) {
                  return Optional.of(p);
                }
              }
              return Optional.empty();
            });
  }

  public CompletableFuture<List<Punishment>> getRecentPunishments(Duration period) {
    return DB.getResultsAsync(
            SELECT_RECENT_QUERY, Instant.now().toEpochMilli() - period.toMillis(), RECENT_LIMIT)
        .thenApplyAsync(
            results -> {
              List<Punishment> punishments = Lists.newArrayList();

              if (results != null && !results.isEmpty()) {
                for (DbRow row : results) {
                  String id = row.getString("id");
                  String target = row.getString("punished");
                  String issuer = row.getString("issuer");
                  String reason = row.getString("reason");
                  String type = row.getString("type");
                  long time = Long.parseLong(row.getString("time"));
                  long expires = Long.parseLong(row.getString("expires"));
                  Instant timeIssued = Instant.ofEpochMilli(time);
                  Duration length =
                      Duration.between(Instant.ofEpochMilli(time), Instant.ofEpochMilli(expires));
                  boolean active = row.get("active");
                  long lastUpdateTime = Long.parseLong(row.getString("last_updated"));
                  Instant lastUpdate = Instant.ofEpochMilli(lastUpdateTime);
                  String lastUpdateBy = row.getString("updated_by");
                  String service = row.getString("service");
                  punishments.add(
                      Punishment.of(
                          UUID.fromString(id),
                          UUID.fromString(target),
                          parseIssuer(issuer),
                          reason,
                          timeIssued,
                          length,
                          PunishmentType.valueOf(type.toUpperCase()),
                          active,
                          lastUpdate,
                          parseIssuer(lastUpdateBy),
                          service));
                }
              }

              return punishments;
            });
  }

  public void invalidate(UUID playerId) {
    if (punishmentCache.getIfPresent(playerId) != null) {
      punishmentCache.invalidate(playerId);
    }
  }

  private class PlayerPunishments {

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
}

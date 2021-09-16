package dev.pgm.community.moderation.services;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import dev.pgm.community.database.DatabaseConnection;
import dev.pgm.community.feature.SQLFeatureBase;
import dev.pgm.community.moderation.ModerationConfig;
import dev.pgm.community.moderation.punishments.Punishment;
import dev.pgm.community.moderation.punishments.PunishmentType;
import dev.pgm.community.moderation.punishments.types.ExpirablePunishment;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import tc.oc.pgm.util.concurrent.ThreadSafeConnection.Query;

public class SQLModerationService extends SQLFeatureBase<Punishment, String> {

  private static final String TABLE_NAME = "punishments";
  private static final String TABLE_FIELDS =
      "(id VARCHAR(36) PRIMARY KEY, punished VARCHAR(36), issuer VARCHAR(36), reason VARCHAR(255), type VARCHAR(8), time LONG, expires LONG, active BOOL, last_updated LONG, updated_by VARCHAR(36), service VARCHAR(255))";
  private static final String CONSOLE_DB_NAME =
      "console"; // Used in issuer field when punishment issued by console

  private LoadingCache<UUID, SelectPunishedQuery> punishmentCache;

  private final ModerationConfig config;

  public SQLModerationService(DatabaseConnection connection, ModerationConfig config) {
    super(connection, TABLE_NAME, TABLE_FIELDS);
    this.config = config;
    this.punishmentCache =
        CacheBuilder.newBuilder()
            .build(
                new CacheLoader<UUID, SelectPunishedQuery>() {
                  @Override
                  public SelectPunishedQuery load(UUID key) throws Exception {
                    return new SelectPunishedQuery(key);
                  }
                });
  }

  @Override
  public void save(Punishment punishment) {
    getDatabase().submitQuery(new InsertQuery(punishment));
  }

  @Override
  public CompletableFuture<List<Punishment>> queryList(String target) {
    SelectPunishedQuery query = punishmentCache.getUnchecked(UUID.fromString(target));
    if (query.hasFetched()) {
      return CompletableFuture.completedFuture(query.getPunishments());
    } else {
      return getDatabase()
          .submitQueryComplete(query)
          .thenApplyAsync(q -> SelectPunishedQuery.class.cast(q).getPunishments());
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
    return getDatabase()
        .submitQueryComplete(new PardonPunishmentQuery(id, issuer, PunishmentType.BAN))
        .thenApplyAsync(query -> PardonPunishmentQuery.class.cast(query).isSuccessful());
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

  public CompletableFuture<Boolean> unmute(UUID id, Optional<UUID> issuer) {
    return getDatabase()
        .submitQueryComplete(new PardonPunishmentQuery(id, issuer, PunishmentType.MUTE))
        .thenApplyAsync(query -> PardonPunishmentQuery.class.cast(query).isSuccessful());
  }

  public CompletableFuture<List<Punishment>> getRecentPunishments(Duration period) {
    return getDatabase()
        .submitQueryComplete(new SelectAllRecentQuery(period, 50))
        .thenApplyAsync(query -> SelectAllRecentQuery.class.cast(query).getPunishments());
  }

  public void invalidate(UUID playerId) {
    if (punishmentCache.getIfPresent(playerId) != null) {
      punishmentCache.invalidate(playerId);
    }
  }

  // Query SubClasses
  private class InsertQuery implements Query {

    private static final String INSERT_PUNISHMENT_QUERY =
        "INSERT INTO "
            + TABLE_NAME
            + "(id, punished, issuer, reason, type, time, expires, active, last_updated, updated_by, service) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private final Punishment punishment;

    public InsertQuery(Punishment punishment) {
      this.punishment = punishment;

      SelectPunishedQuery cached = punishmentCache.getIfPresent(punishment.getTargetId());
      if (cached != null && cached.hasFetched()) {
        cached.getPunishments().add(punishment);
      }
    }

    @Override
    public String getFormat() {
      return INSERT_PUNISHMENT_QUERY;
    }

    @Override
    public void query(PreparedStatement statement) throws SQLException {
      statement.setString(1, UUID.randomUUID().toString());
      statement.setString(2, punishment.getTargetId().toString());
      statement.setString(3, convertIssuer(punishment.getIssuerId()));
      statement.setString(4, punishment.getReason());
      statement.setString(5, punishment.getType().toString());
      statement.setLong(6, punishment.getTimeIssued().toEpochMilli());
      statement.setLong(7, getExpiration(punishment));
      statement.setBoolean(8, punishment.isActive());
      statement.setLong(9, punishment.getTimeIssued().toEpochMilli());
      statement.setString(10, convertIssuer(punishment.getIssuerId()));
      statement.setString(11, config.getService());

      statement.executeUpdate();
    }
  }

  private class SelectAllRecentQuery implements Query {
    private static final String SELECT_QUERY =
        "SELECT * from " + TABLE_NAME + " WHERE time > ? LIMIT ?";

    private List<Punishment> punishments;
    private long period;
    private int limit;

    public SelectAllRecentQuery(Duration timePeriod, int max) {
      this.period = Instant.now().toEpochMilli() - timePeriod.toMillis();
      this.limit = max;
      this.punishments = Lists.newArrayList();
    }

    public List<Punishment> getPunishments() {
      return punishments;
    }

    @Override
    public String getFormat() {
      return SELECT_QUERY;
    }

    @Override
    public void query(PreparedStatement statement) throws SQLException {
      statement.setLong(1, period);
      statement.setInt(2, limit);

      try (final ResultSet result = statement.executeQuery()) {
        while (result.next()) {
          String id = result.getString("id");
          String target = result.getString("punished");
          String issuer = result.getString("issuer");
          String reason = result.getString("reason");
          String type = result.getString("type");
          long time = result.getLong("time");
          long expires = result.getLong("expires");
          Instant timeIssued = Instant.ofEpochMilli(time);
          Duration length =
              Duration.between(Instant.ofEpochMilli(time), Instant.ofEpochMilli(expires));
          boolean active = result.getBoolean("active");
          long lastUpdateTime = result.getLong("last_updated");
          Instant lastUpdate = Instant.ofEpochMilli(lastUpdateTime);
          String lastUpdateBy = result.getString("updated_by");
          String service = result.getString("service");
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
    }
  }

  private class SelectPunishedQuery implements Query {

    private static final String SELECT_QUERY =
        "SELECT * from " + TABLE_NAME + " where punished = ?";

    private List<Punishment> punishments;
    private UUID target;
    private boolean fetched;

    public SelectPunishedQuery(UUID target) {
      this.target = target;
      this.punishments = Lists.newArrayList();
      this.fetched = false;
    }

    public boolean hasFetched() {
      return fetched;
    }

    public List<Punishment> getPunishments() {
      return punishments;
    }

    @Override
    public String getFormat() {
      return SELECT_QUERY;
    }

    @Override
    public void query(PreparedStatement statement) throws SQLException {
      if (fetched) return;

      statement.setString(1, target.toString());
      try (final ResultSet result = statement.executeQuery()) {
        while (result.next()) {
          String id = result.getString("id");
          String issuer = result.getString("issuer");
          String reason = result.getString("reason");
          String type = result.getString("type");
          long time = result.getLong("time");
          long expires = result.getLong("expires");
          Instant timeIssued = Instant.ofEpochMilli(time);
          Duration length =
              Duration.between(Instant.ofEpochMilli(time), Instant.ofEpochMilli(expires));
          boolean active = result.getBoolean("active");
          long lastUpdateTime = result.getLong("last_updated");
          Instant lastUpdate = Instant.ofEpochMilli(lastUpdateTime);
          String lastUpdateBy = result.getString("updated_by");
          String service = result.getString("service");
          punishments.add(
              Punishment.of(
                  UUID.fromString(id),
                  target,
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
        fetched = true;
      }
    }
  }

  private class PardonPunishmentQuery implements Query {
    private static final String SINGLE_TYPE = "AND type = ?";
    private static final String MULTI_TYPE = "AND (type = ? OR type = ? OR type = ?)";
    private static final String QUERY_FORMAT =
        "UPDATE "
            + TABLE_NAME
            + " SET active = ?, last_updated = ?, updated_by = ? WHERE active = ? AND punished = ? ";

    private UUID id;
    private Optional<UUID> issuer;
    private boolean successful;
    private boolean ban;

    public PardonPunishmentQuery(UUID id, Optional<UUID> issuer, PunishmentType type) {
      this.id = id;
      this.issuer = issuer;
      this.ban = type.isLoginPrevented();

      punishmentCache.invalidate(id);
    }

    public boolean isSuccessful() {
      return successful;
    }

    @Override
    public String getFormat() {
      return QUERY_FORMAT + (ban ? MULTI_TYPE : SINGLE_TYPE);
    }

    @Override
    public void query(PreparedStatement statement) throws SQLException {
      statement.setBoolean(1, false);
      statement.setLong(2, Instant.now().toEpochMilli());
      statement.setString(3, convertIssuer(issuer));

      statement.setBoolean(4, true);
      statement.setString(5, id.toString());

      if (ban) {
        statement.setString(6, PunishmentType.BAN.toString());
        statement.setString(7, PunishmentType.TEMP_BAN.toString());
        statement.setString(8, PunishmentType.NAME_BAN.toString());
      } else {
        statement.setString(6, PunishmentType.MUTE.toString());
      }

      int updated = statement.executeUpdate();
      successful = updated != 0;
    }
  }
}

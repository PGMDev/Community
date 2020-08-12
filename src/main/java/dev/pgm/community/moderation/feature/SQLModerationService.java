package dev.pgm.community.moderation.feature;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import dev.pgm.community.Community;
import dev.pgm.community.database.DatabaseConnection;
import dev.pgm.community.feature.SQLFeatureBase;
import dev.pgm.community.moderation.ModerationConfig;
import dev.pgm.community.moderation.punishments.Punishment;
import dev.pgm.community.moderation.punishments.PunishmentType;
import dev.pgm.community.moderation.punishments.types.ExpirablePunishment;
import dev.pgm.community.usernames.UsernameService;
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

public class SQLModerationService extends SQLFeatureBase<Punishment> {

  private static final String TABLE_NAME = "punishments";
  private static final String TABLE_FIELDS =
      "(id VARCHAR(36) PRIMARY KEY, punished VARCHAR(36), issuer VARCHAR(36), reason VARCHAR(255), type VARCHAR(8), time LONG, expires LONG, active BOOL, last_updated LONG, updated_by VARCHAR(36))";

  private LoadingCache<UUID, SelectPunishedQuery> punishmentCache;

  private final ModerationConfig config;
  private final UsernameService usernames;

  public SQLModerationService(
      DatabaseConnection connection, ModerationConfig config, UsernameService usernames) {
    super(connection, TABLE_NAME, TABLE_FIELDS);
    this.config = config;
    this.usernames = usernames;
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
  public CompletableFuture<List<Punishment>> query(String target) {
    SelectPunishedQuery query = punishmentCache.getUnchecked(UUID.fromString(target));
    if (query.hasFetched()) {
      Community.log("Punishments were &acached&7, now returning cached results");
      return CompletableFuture.completedFuture(query.getPunishments());
    } else {
      Community.log("Punishments &cNOT FOUND&7 cached, now submitting queue to database...");
      return getDatabase()
          .submitQueryComplete(query)
          .thenApplyAsync(q -> SelectPunishedQuery.class.cast(q).getPunishments());
    }
  }

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

  private class InsertQuery implements Query {

    private static final String INSERT_PUNISHMENT_QUERY =
        "INSERT INTO "
            + TABLE_NAME
            + "(id, punished, issuer, reason, type, time, expires, active, last_updated, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

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

      statement.executeUpdate();
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
                  config,
                  usernames));
        }
        fetched = true;
      }
    }
  }

  private class PardonPunishmentQuery implements Query {
    private static final String UPDATE_QUERY =
        "UPDATE "
            + TABLE_NAME
            + " SET active = ?, last_updated = ?, updated_by = ? WHERE active = ? AND punished = ? AND (type = ? OR type = ?)";

    private UUID id;
    private Optional<UUID> issuer;
    private boolean successful;

    public PardonPunishmentQuery(UUID id, Optional<UUID> issuer) {
      this.id = id;
      this.issuer = issuer;
      punishmentCache.invalidate(id);
    }

    public boolean isSuccessful() {
      return successful;
    }

    @Override
    public String getFormat() {
      return UPDATE_QUERY;
    }

    @Override
    public void query(PreparedStatement statement) throws SQLException {
      statement.setBoolean(1, false);
      statement.setLong(2, Instant.now().toEpochMilli());
      statement.setString(3, convertIssuer(issuer));

      statement.setBoolean(4, true);
      statement.setString(5, id.toString());
      statement.setString(6, PunishmentType.BAN.toString());
      statement.setString(7, PunishmentType.TEMP_BAN.toString());

      int updated = statement.executeUpdate();
      successful = updated != 0;
      Community.log("Updated " + updated + " punishments for " + id.toString());
    }
  }

  private static final String CONSOLE_DB_NAME = "console";

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
        .submitQueryComplete(new PardonPunishmentQuery(id, issuer))
        .thenApplyAsync(query -> PardonPunishmentQuery.class.cast(query).isSuccessful());
  }

  public CompletableFuture<Boolean> isBanned(String id) {
    return query(id)
        .thenApplyAsync(
            punishments -> {
              boolean banned = false;
              for (Punishment p : punishments) {
                if (PunishmentType.isBan(p.getType()) && p.isActive()) {
                  banned = true;
                  break;
                }
              }
              return banned;
            });
  }

  public CompletableFuture<PunishmentType> getNextPunishment(UUID target) {
    return query(target.toString())
        .thenApplyAsync(
            punishments -> {
              if (punishments.isEmpty()) return PunishmentType.WARN;
              Community.log("Most Recent == " + punishments.get(0).getReason());
              // NOTE: Ideal punishment history Warn -> Kick -> TempBan -> Ban

              switch (punishments.get(0).getType()) {
                case KICK:
                  return PunishmentType.TEMP_BAN;
                case MUTE:
                  return PunishmentType.KICK;
                case TEMP_BAN:
                  return PunishmentType.BAN;
                case WARN:
                  return PunishmentType.KICK;
                default:
                  return PunishmentType.BAN;
              }
            });
  }
}

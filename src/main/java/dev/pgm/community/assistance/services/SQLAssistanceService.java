package dev.pgm.community.assistance.services;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import dev.pgm.community.Community;
import dev.pgm.community.assistance.Report;
import dev.pgm.community.database.DatabaseConnection;
import dev.pgm.community.feature.SQLFeatureBase;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import tc.oc.pgm.util.concurrent.ThreadSafeConnection.Query;

public class SQLAssistanceService extends SQLFeatureBase<Report> {

  private static final String TABLE_NAME = "reports";
  private static final String TABLE_FIELDS =
      "(id VARCHAR(36) PRIMARY KEY, sender VARCHAR(36), reported VARCHAR(36), reason VARCHAR(255), time LONG, server VARCHAR(255))";

  private LoadingCache<UUID, SelectQuery> cachedReports;

  public SQLAssistanceService(DatabaseConnection database) {
    super(database, TABLE_NAME, TABLE_FIELDS);
    this.cachedReports =
        CacheBuilder.newBuilder()
            .maximumSize(1000)
            .build(
                new CacheLoader<UUID, SelectQuery>() {
                  @Override
                  public SelectQuery load(UUID key) throws Exception {
                    return new SelectQuery(key);
                  }
                });
  }

  @Override
  public void save(Report report) {
    getDatabase().submitQuery(new InsertQuery(report));
  }

  @Override
  public CompletableFuture<List<Report>> queryList(String target) {
    SelectQuery query = cachedReports.getUnchecked(UUID.fromString(target));
    if (query.hasFetched()) {
      return CompletableFuture.completedFuture(query.getReports());
    } else {
      return getDatabase()
          .submitQueryComplete(query)
          .thenApplyAsync(q -> SelectQuery.class.cast(q).getReports());
    }
  }

  @Override
  public CompletableFuture<Report> query(String target) {
    return CompletableFuture.completedFuture(null); // Noop atm
  }

  private class InsertQuery implements Query {

    private static final String INSERT_REPORT_QUERY =
        "INSERT INTO "
            + TABLE_NAME
            + "(id, sender, reported, reason, time, server) VALUES (?, ?, ?, ?, ?, ?)";

    private final Report report;

    public InsertQuery(Report report) {
      this.report = report;

      // Cache newly saved reports to prevent further lookups while server is online
      SelectQuery cached = cachedReports.getIfPresent(report.getTargetId());
      if (cached != null) {
        cached.getReports().add(report);
      }
    }

    @Override
    public String getFormat() {
      return INSERT_REPORT_QUERY;
    }

    @Override
    public void query(PreparedStatement statement) throws SQLException {
      statement.setString(1, report.getId().toString());
      statement.setString(2, report.getSenderId().toString());
      statement.setString(3, report.getTargetId().toString());
      statement.setString(4, report.getReason());
      statement.setLong(5, report.getTime().toEpochMilli());
      statement.setString(6, Community.get().getServerConfig().getServerId());
      statement.executeUpdate();
    }
  }

  private class SelectQuery implements Query {
    private static final String SELECT_REPORT_QUERY =
        "SELECT id, sender, reason, time FROM " + TABLE_NAME + " WHERE reported = ?";

    private UUID targetId;
    private List<Report> reports;
    private boolean fetched;

    public SelectQuery(UUID targetId) {
      this.targetId = targetId;
      this.reports = Lists.newArrayList();
      this.fetched = false;
    }

    @Override
    public String getFormat() {
      return SELECT_REPORT_QUERY;
    }

    public List<Report> getReports() {
      return reports;
    }

    public boolean hasFetched() {
      return fetched;
    }

    @Override
    public void query(PreparedStatement statement) throws SQLException {
      if (fetched) return;
      statement.setString(1, targetId.toString());

      try (final ResultSet result = statement.executeQuery()) {
        while (result.next()) {
          String id = result.getString("id");
          String sender = result.getString("sender");
          String reason = result.getString("reason");
          long time = result.getLong("time");

          getReports()
              .add(
                  new Report(
                      UUID.fromString(id),
                      targetId,
                      UUID.fromString(sender),
                      reason,
                      Instant.ofEpochMilli(time),
                      Community.get().getServerConfig().getServerId()));
        }
        fetched = true;
      }
    }
  }

  public void invalidate(UUID playerId) {
    cachedReports.invalidate(playerId);
  }
}

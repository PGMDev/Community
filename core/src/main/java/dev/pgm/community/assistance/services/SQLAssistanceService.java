package dev.pgm.community.assistance.services;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import dev.pgm.community.Community;
import dev.pgm.community.assistance.Report;
import dev.pgm.community.database.DatabaseExecutor;
import dev.pgm.community.feature.SQLFeatureBase;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SQLAssistanceService extends SQLFeatureBase<Report, String>
    implements AssistanceQuery {

  private final LoadingCache<UUID, PlayerReports> cachedReports;

  public SQLAssistanceService() {
    super(TABLE_NAME, TABLE_FIELDS);
    this.cachedReports =
        CacheBuilder.newBuilder().maximumSize(1000).build(CacheLoader.from(PlayerReports::new));
  }

  @Override
  public void save(Report report) {
    PlayerReports playerReports = cachedReports.getUnchecked(report.getTargetId());
    playerReports.getReports().add(report);

    DatabaseExecutor.executeUpdateAsync(
        INSERT_REPORT_QUERY,
        report.getId().toString(),
        report.getSenderId().toString(),
        report.getTargetId().toString(),
        report.getReason(),
        report.getTime().toEpochMilli(),
        Community.get().getServerConfig().getServerId());
  }

  @Override
  public CompletableFuture<List<Report>> queryList(String target) {
    UUID targetId = UUID.fromString(target);
    PlayerReports reports = cachedReports.getUnchecked(targetId);

    if (reports.isLoaded()) {
      return CompletableFuture.completedFuture(reports.getReports());
    } else {
      return DatabaseExecutor.queryAsync(
              SELECT_REPORT_QUERY,
              row -> {
                String id = row.getString("id");
                String sender = row.getString("sender");
                String reason = row.getString("reason");
                long time = row.getLong("time");
                return new Report(
                    UUID.fromString(id),
                    targetId,
                    UUID.fromString(sender),
                    reason,
                    time,
                    Community.get().getServerConfig().getServerId());
              },
              target)
          .thenApplyAsync(results -> {
            if (results != null) {
              reports.getReports().addAll(results);
            }
            reports.setLoaded(true);
            return reports.getReports();
          });
    }
  }

  @Override
  public CompletableFuture<Report> query(String target) {
    return CompletableFuture.completedFuture(null); // Noop atm
  }

  private static class PlayerReports {
    private final UUID playerId;
    private final List<Report> reports;
    private boolean loaded;

    public PlayerReports(UUID playerId) {
      this.playerId = playerId;
      this.reports = Lists.newArrayList();
      this.loaded = false;
    }

    public UUID getPlayerId() {
      return playerId;
    }

    public List<Report> getReports() {
      return reports;
    }

    public boolean isLoaded() {
      return loaded;
    }

    public void setLoaded(boolean loaded) {
      this.loaded = loaded;
    }
  }

  public void invalidate(UUID playerId) {
    cachedReports.invalidate(playerId);
  }
}

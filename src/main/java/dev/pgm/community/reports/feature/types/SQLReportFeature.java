package dev.pgm.community.reports.feature.types;

import co.aikar.idb.DB;
import com.google.common.collect.Lists;
import dev.pgm.community.reports.Report;
import dev.pgm.community.reports.ReportConfig;
import dev.pgm.community.reports.feature.ReportFeatureBase;
import dev.pgm.community.usernames.UsernameService;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;

public class SQLReportFeature extends ReportFeatureBase {

  private static final String TABLE_NAME = "reports";

  private static final String INSERT_REPORT_QUERY =
      "INSERT INTO " + TABLE_NAME + "(id, sender, reported, reason, time) VALUES (?, ?, ?, ?, ?)";
  private static final String SELECT_REPORT_QUERY =
      "SELECT sender, reason, time FROM reports WHERE reported = ?";
  private static final String TABLE_CREATE_QUERY =
      "CREATE TABLE IF NOT EXISTS %s (id VARCHAR(36) PRIMARY KEY, sender VARCHAR(36), reported VARCHAR(36), reason VARCHAR(255), time LONG)";

  public SQLReportFeature(Configuration config, Logger logger, UsernameService usernames) {
    super(new ReportConfig(config), logger, usernames);
    createTable();
  }

  @Override
  public void enable() {
    logger.info("Reports (SQL) have been enabled");
    super.enable();
  }

  private void createTable() {
    try {
      DB.executeUpdate(String.format(TABLE_CREATE_QUERY, TABLE_NAME));
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  @Override
  public Report report(Player sender, Player target, String reason) {
    Report report = super.report(sender, target, reason);
    if (isPersistent()) {
      try {
        DB.executeInsert(
            INSERT_REPORT_QUERY,
            UUID.randomUUID(),
            report.getReporterId().toString(),
            report.getReportedId().toString(),
            reason,
            report.getTime().toEpochMilli());
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
    return report;
  }

  @Override
  public CompletableFuture<List<Report>> query(String target) {
    if (UsernameService.USERNAME_REGEX.matcher(target).matches()) {
      // CONVERT TO UUID if username
      Optional<UUID> cachedId = usernames.getId(target);
      if (cachedId.isPresent()) {
        target = cachedId.get().toString();
      } else {
        return CompletableFuture.completedFuture(Lists.newArrayList());
      }
    }

    final String targetId = target;
    return DB.getResultsAsync(SELECT_REPORT_QUERY, targetId)
        .thenApply(
            results -> {
              List<Report> reports = Lists.newArrayList();
              results.forEach(
                  result -> {
                    UUID reported = UUID.fromString(targetId);
                    UUID sender = UUID.fromString(result.getString("sender"));
                    String reason = result.getString("reason");
                    long milli = result.getLong("time");
                    Instant time = Instant.ofEpochMilli(milli);
                    reports.add(new Report(reported, sender, reason, time));
                  });
              return reports;
            });
  }
}

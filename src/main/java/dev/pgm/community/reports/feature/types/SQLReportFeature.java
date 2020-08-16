package dev.pgm.community.reports.feature.types;

import com.google.common.collect.Lists;
import dev.pgm.community.database.DatabaseConnection;
import dev.pgm.community.reports.Report;
import dev.pgm.community.reports.ReportConfig;
import dev.pgm.community.reports.feature.ReportFeatureBase;
import dev.pgm.community.reports.services.SQLReportService;
import dev.pgm.community.users.feature.UsersFeature;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;

public class SQLReportFeature extends ReportFeatureBase {

  private final SQLReportService service;
  private final UsersFeature usernames;

  public SQLReportFeature(
      Configuration config, Logger logger, DatabaseConnection database, UsersFeature usernames) {
    super(new ReportConfig(config), logger);
    this.service = new SQLReportService(database);
    this.usernames = usernames;
  }

  @Override
  public void enable() {
    logger.info("Reports (SQL) have been enabled");
    super.enable();
  }

  @Override
  public Report report(Player sender, Player target, String reason) {
    Report report = super.report(sender, target, reason);
    if (isPersistent()) {
      service.save(report);
    }
    return report;
  }

  @Override
  public CompletableFuture<List<Report>> query(String target) {
    if (UsersFeature.USERNAME_REGEX.matcher(target).matches()) {
      // CONVERT TO UUID if username
      return usernames
          .getStoredId(target)
          .thenApplyAsync(
              uuid ->
                  uuid.isPresent()
                      ? service.queryList(uuid.get().toString()).join()
                      : Lists.newArrayList());
    }

    return service.queryList(target);
  }
}

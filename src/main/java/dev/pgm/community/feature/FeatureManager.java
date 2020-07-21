package dev.pgm.community.feature;

import dev.pgm.community.CommunityConfig.DatabaseType;
import dev.pgm.community.reports.feature.ReportFeature;
import dev.pgm.community.reports.feature.types.NoDBReportFeature;
import dev.pgm.community.reports.feature.types.SQLReportFeature;
import dev.pgm.community.usernames.UsernameService;
import java.util.logging.Logger;
import org.bukkit.configuration.Configuration;

public class FeatureManager {

  private final ReportFeature reports;

  public FeatureManager(
      Configuration config, DatabaseType type, Logger logger, UsernameService username) {
    this.reports =
        type.equals(DatabaseType.NONE)
            ? new NoDBReportFeature(config, logger, username)
            : new SQLReportFeature(config, logger, username);
    // TODO?: Detect type of database, and create appropriate feature type.
    // Ex. FileReportFeature, MongoReportFeature, RedisReportFeature...
    // Not a priority though
  }

  public ReportFeature getReports() {
    return reports;
  }

  public void reloadConfig() {
    // Reload all config values here
    reports.getConfig().reload();
  }
}

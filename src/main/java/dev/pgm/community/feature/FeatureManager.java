package dev.pgm.community.feature;

import dev.pgm.community.reports.ReportFeature;
import dev.pgm.community.reports.SQLReportFeature;
import org.bukkit.configuration.Configuration;

public class FeatureManager {

  private ReportFeature reports;

  public FeatureManager(Configuration config) {
    this.reports = new SQLReportFeature(config);
    // TODO?: Detect type of database, and create appropriate feature type.
    // Ex. FileReportFeature, MongoReportFeature, RedisReportFeature...
  }

  public ReportFeature getReports() {
    return reports;
  }

  public void reloadConfig() {
    // Reload all config values here
    reports.getConfig().reload();
  }
}

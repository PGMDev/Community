package dev.pgm.community.reports;

import dev.pgm.community.feature.FeatureBase;
import java.util.concurrent.CompletableFuture;
import org.bukkit.configuration.Configuration;

public class SQLReportFeature extends FeatureBase implements ReportFeature {

  public SQLReportFeature(Configuration config) {
    super(new ReportConfig(config));
  }

  @Override
  public CompletableFuture<Report> report() { // TODO:
    return null;
  }
}

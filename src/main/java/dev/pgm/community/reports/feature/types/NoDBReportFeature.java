package dev.pgm.community.reports.feature.types;

import com.google.common.collect.Lists;
import dev.pgm.community.reports.Report;
import dev.pgm.community.reports.ReportConfig;
import dev.pgm.community.reports.feature.ReportFeatureBase;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.bukkit.configuration.Configuration;

public class NoDBReportFeature extends ReportFeatureBase {

  public NoDBReportFeature(Configuration config, Logger logger) {
    super(new ReportConfig(config), logger);
  }

  @Override
  public CompletableFuture<List<Report>> query(String target) {
    return CompletableFuture.completedFuture(Lists.newArrayList());
  }
}

package dev.pgm.community.reports.feature.types;

import com.google.common.collect.Lists;
import dev.pgm.community.reports.Report;
import dev.pgm.community.reports.ReportConfig;
import dev.pgm.community.reports.feature.ReportFeatureBase;
import dev.pgm.community.usernames.UsernameService;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.bukkit.configuration.Configuration;

public class NoDBReportFeature extends ReportFeatureBase {

  public NoDBReportFeature(Configuration config, Logger logger, UsernameService usernames) {
    super(new ReportConfig(config), logger, usernames);
  }

  @Override
  public CompletableFuture<List<Report>> query(String target) {
    return CompletableFuture.completedFuture(Lists.newArrayList());
  }
}

package dev.pgm.community.assistance.feature.types;

import com.google.common.collect.Lists;
import dev.pgm.community.assistance.Report;
import dev.pgm.community.assistance.ReportConfig;
import dev.pgm.community.assistance.feature.AssistanceFeatureBase;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.bukkit.configuration.Configuration;

public class NoDBAssistanceFeature extends AssistanceFeatureBase {

  public NoDBAssistanceFeature(Configuration config, Logger logger) {
    super(new ReportConfig(config), logger, "Assistance (No Database)");
  }

  @Override
  public CompletableFuture<List<Report>> query(String target) {
    return CompletableFuture.completedFuture(Lists.newArrayList());
  }
}

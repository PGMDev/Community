package dev.pgm.community.assistance.feature.types;

import com.google.common.collect.Lists;
import dev.pgm.community.assistance.Report;
import dev.pgm.community.assistance.ReportConfig;
import dev.pgm.community.assistance.feature.AssistanceFeatureBase;
import dev.pgm.community.network.feature.NetworkFeature;
import dev.pgm.community.users.feature.UsersFeature;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.bukkit.configuration.Configuration;

public class NoDBAssistanceFeature extends AssistanceFeatureBase {

  public NoDBAssistanceFeature(
      Configuration config, Logger logger, NetworkFeature network, UsersFeature users) {
    super(new ReportConfig(config), logger, "Assistance (No Database)", network, users);
  }

  @Override
  public CompletableFuture<List<Report>> query(String target) {
    return CompletableFuture.completedFuture(Lists.newArrayList());
  }

  @Override
  public void invalidate(UUID playerId) {
    // No-op
  }
}

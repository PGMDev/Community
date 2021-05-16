package dev.pgm.community.assistance.feature.types;

import com.google.common.collect.Lists;
import dev.pgm.community.assistance.Report;
import dev.pgm.community.assistance.ReportConfig;
import dev.pgm.community.assistance.feature.AssistanceFeatureBase;
import dev.pgm.community.assistance.services.SQLAssistanceService;
import dev.pgm.community.database.DatabaseConnection;
import dev.pgm.community.network.feature.NetworkFeature;
import dev.pgm.community.users.feature.UsersFeature;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;

public class SQLAssistanceFeature extends AssistanceFeatureBase {

  private final SQLAssistanceService service;

  public SQLAssistanceFeature(
      Configuration config,
      Logger logger,
      DatabaseConnection database,
      UsersFeature usernames,
      NetworkFeature network) {
    super(new ReportConfig(config), logger, "Assistance (SQL)", network, usernames);
    this.service = new SQLAssistanceService(database);
  }

  @Override
  public void enable() {
    logger.info("Reports (SQL) have been enabled");
    super.enable();
  }

  @Override
  public Report report(Player sender, Player target, String reason) {
    Report report = super.report(sender, target, reason);
    if (isPersistent() && report != null) {
      service.save(report);
    }
    return report;
  }

  @Override
  public CompletableFuture<List<Report>> query(String target) {
    if (UsersFeature.USERNAME_REGEX.matcher(target).matches()) {
      // CONVERT TO UUID if username
      return users
          .getStoredId(target)
          .thenApplyAsync(
              uuid ->
                  uuid.isPresent()
                      ? service.queryList(uuid.get().toString()).join()
                      : Lists.newArrayList());
    }

    return service.queryList(target);
  }

  @Override
  public CompletableFuture<Integer> count() {
    return service.count();
  }

  @Override
  public void invalidate(UUID playerId) {
    service.invalidate(playerId);
  }
}

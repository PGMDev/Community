package dev.pgm.community.feature;

import co.aikar.commands.BukkitCommandManager;
import dev.pgm.community.database.DatabaseConnection;
import dev.pgm.community.moderation.feature.ModerationFeature;
import dev.pgm.community.moderation.feature.types.SQLModerationFeature;
import dev.pgm.community.reports.feature.ReportFeature;
import dev.pgm.community.reports.feature.types.SQLReportFeature;
import dev.pgm.community.usernames.UsernameService;
import java.util.logging.Logger;
import org.bukkit.configuration.Configuration;

public class FeatureManager {

  private final ReportFeature reports;
  private final ModerationFeature moderation;

  public FeatureManager(
      Configuration config,
      Logger logger,
      DatabaseConnection database,
      UsernameService username,
      BukkitCommandManager commands) {
    this.reports = new SQLReportFeature(config, logger, database, username);
    this.moderation = new SQLModerationFeature(config, logger, database, username);
    // TODO: Support non-sql databases?
    // Ex. FileReportFeature, MongoReportFeature, RedisReportFeature...
    // Not a priority

    this.registerCommands(commands);
  }

  public ReportFeature getReports() {
    return reports;
  }

  public ModerationFeature getModeration() {
    return moderation;
  }

  // Register Feature commands and any dependency
  private void registerCommands(BukkitCommandManager commands) {
    commands.registerDependency(ReportFeature.class, getReports());
    commands.registerDependency(ModerationFeature.class, getModeration());

    getReports().getCommands().forEach(commands::registerCommand);
    getModeration().getCommands().forEach(commands::registerCommand);
  }

  public void reloadConfig() {
    // Reload all config values here
    getReports().getConfig().reload();
    getModeration().getConfig().reload();
    // TODO: Look into maybe unregister commands for features that have been disabled
    // commands#unregisterCommand
    // Will need to check isEnabled
  }
}

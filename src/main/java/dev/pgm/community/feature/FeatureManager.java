package dev.pgm.community.feature;

import co.aikar.commands.BukkitCommandManager;
import dev.pgm.community.commands.CommunityPluginCommand;
import dev.pgm.community.database.DatabaseConnection;
import dev.pgm.community.moderation.feature.ModerationFeature;
import dev.pgm.community.moderation.feature.types.SQLModerationFeature;
import dev.pgm.community.reports.feature.ReportFeature;
import dev.pgm.community.reports.feature.types.SQLReportFeature;
import dev.pgm.community.teleports.TeleportFeature;
import dev.pgm.community.teleports.TeleportFeatureBase;
import dev.pgm.community.users.feature.UsersFeature;
import dev.pgm.community.users.feature.types.SQLUsersFeature;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;

public class FeatureManager {

  private final ReportFeature reports;
  private final ModerationFeature moderation;
  private final UsersFeature users;

  private final TeleportFeature teleports;

  public FeatureManager(
      Configuration config,
      Logger logger,
      DatabaseConnection database,
      BukkitCommandManager commands) {
    // DB Features
    this.users = new SQLUsersFeature(config, logger, database);
    this.reports = new SQLReportFeature(config, logger, database, users);
    this.moderation = new SQLModerationFeature(config, logger, database, users);
    // TODO: 1. Add support for non-persist database (e.g NoDBUsersFeature)
    // TODO: 2. Support non-sql databases?
    // Ex. FileReportFeature, MongoReportFeature, RedisReportFeature...
    // Not a priority

    // Non-DB Features
    this.teleports = new TeleportFeatureBase(config, logger);

    this.registerCommands(commands);
  }

  public ReportFeature getReports() {
    return reports;
  }

  public ModerationFeature getModeration() {
    return moderation;
  }

  public UsersFeature getUsers() {
    return users;
  }

  public TeleportFeature getTeleports() {
    return teleports;
  }

  // Register Feature commands and any dependency
  private void registerCommands(BukkitCommandManager commands) {
    // Dependency injection for features
    commands.registerDependency(UsersFeature.class, getUsers());
    commands.registerDependency(ReportFeature.class, getReports());
    commands.registerDependency(ModerationFeature.class, getModeration());
    commands.registerDependency(TeleportFeature.class, getTeleports());

    // Custom command completions
    commands
        .getCommandCompletions()
        .registerCompletion(
            "mutes",
            x ->
                getModeration().getOnlineMutes().stream()
                    .map(Player::getName)
                    .collect(Collectors.toSet()));

    // Feature commands
    getUsers().getCommands().forEach(commands::registerCommand);
    getReports().getCommands().forEach(commands::registerCommand);
    getModeration().getCommands().forEach(commands::registerCommand);
    getTeleports().getCommands().forEach(commands::registerCommand);

    // Other commands
    commands.registerCommand(new CommunityPluginCommand());
  }

  public void reloadConfig() {
    // Reload all config values here
    getReports().getConfig().reload();
    getModeration().getConfig().reload();
    getUsers().getConfig().reload();
    getTeleports().getConfig().reload();
    // TODO: Look into maybe unregister commands for features that have been disabled
    // commands#unregisterCommand
    // Will need to check isEnabled
  }
}

package dev.pgm.community.feature;

import co.aikar.commands.BukkitCommandManager;
import dev.pgm.community.chat.ChatManagementFeature;
import dev.pgm.community.commands.CommunityPluginCommand;
import dev.pgm.community.database.DatabaseConnection;
import dev.pgm.community.friends.feature.FriendshipFeature;
import dev.pgm.community.friends.feature.types.SQLFriendshipFeature;
import dev.pgm.community.info.InfoCommandsFeature;
import dev.pgm.community.moderation.feature.ModerationFeature;
import dev.pgm.community.moderation.feature.types.SQLModerationFeature;
import dev.pgm.community.motd.MotdFeature;
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
  private final FriendshipFeature friends;

  private final TeleportFeature teleports;
  private final InfoCommandsFeature infoCommands;
  private final ChatManagementFeature chat;
  private final MotdFeature motd;

  public FeatureManager(
      Configuration config,
      Logger logger,
      DatabaseConnection database,
      BukkitCommandManager commands) {
    // DB Features
    this.users = new SQLUsersFeature(config, logger, database);
    this.reports = new SQLReportFeature(config, logger, database, users);
    this.moderation = new SQLModerationFeature(config, logger, database, users);
    this.friends = new SQLFriendshipFeature(config, logger, database, users);
    // TODO: 1. Add support for non-persist database (e.g NoDBUsersFeature)
    // TODO: 2. Support non-sql databases?
    // Ex. FileReportFeature, MongoReportFeature, RedisReportFeature...
    // Not a priority

    // Non-DB Features
    this.teleports = new TeleportFeatureBase(config, logger);
    this.infoCommands = new InfoCommandsFeature(config, logger);
    this.chat = new ChatManagementFeature(config, logger);
    this.motd = new MotdFeature(config, logger);

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

  public InfoCommandsFeature getInfoCommands() {
    return infoCommands;
  }

  public ChatManagementFeature getChatManagement() {
    return chat;
  }

  public FriendshipFeature getFriendships() {
    return friends;
  }

  public MotdFeature getMotd() {
    return motd;
  }

  // Register Feature commands and any dependency
  private void registerCommands(BukkitCommandManager commands) {
    // Dependency injection for features
    commands.registerDependency(UsersFeature.class, getUsers());
    commands.registerDependency(ReportFeature.class, getReports());
    commands.registerDependency(ModerationFeature.class, getModeration());
    commands.registerDependency(TeleportFeature.class, getTeleports());
    commands.registerDependency(ChatManagementFeature.class, getChatManagement());
    commands.registerDependency(FriendshipFeature.class, getFriendships());

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
    registerFeatureCommands(getUsers(), commands);
    registerFeatureCommands(getReports(), commands);
    registerFeatureCommands(getModeration(), commands);
    registerFeatureCommands(getTeleports(), commands);
    registerFeatureCommands(getChatManagement(), commands);
    registerFeatureCommands(getFriendships(), commands);
    // TODO: Group calls together and perform upon reload
    // will allow commands to be enabled/disabled with features

    // Other commands
    commands.registerCommand(new CommunityPluginCommand());
  }

  private void registerFeatureCommands(Feature feature, BukkitCommandManager commandManager) {
    feature.getCommands().forEach(commandManager::registerCommand);
  }

  public void reloadConfig(Configuration config) {
    // Reload all config values here
    getReports().getConfig().reload(config);
    getModeration().getConfig().reload(config);
    getUsers().getConfig().reload(config);
    getTeleports().getConfig().reload(config);
    getInfoCommands().getConfig().reload(config);
    getChatManagement().getConfig().reload(config);
    getMotd().getConfig().reload(config);
    // TODO: Look into maybe unregister commands for features that have been disabled
    // commands#unregisterCommand
    // Will need to check isEnabled
  }
}

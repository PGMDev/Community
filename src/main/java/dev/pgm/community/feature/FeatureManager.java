package dev.pgm.community.feature;

import co.aikar.commands.BukkitCommandManager;
import dev.pgm.community.assistance.feature.AssistanceFeature;
import dev.pgm.community.assistance.feature.types.SQLAssistanceFeature;
import dev.pgm.community.broadcast.BroadcastFeature;
import dev.pgm.community.chat.ChatManagementFeature;
import dev.pgm.community.commands.CommunityPluginCommand;
import dev.pgm.community.commands.FlightCommand;
import dev.pgm.community.commands.GamemodeCommand;
import dev.pgm.community.commands.StaffCommand;
import dev.pgm.community.database.DatabaseConnection;
import dev.pgm.community.freeze.FreezeFeature;
import dev.pgm.community.friends.feature.FriendshipFeature;
import dev.pgm.community.friends.feature.types.SQLFriendshipFeature;
import dev.pgm.community.info.InfoCommandsFeature;
import dev.pgm.community.moderation.feature.ModerationFeature;
import dev.pgm.community.moderation.feature.types.SQLModerationFeature;
import dev.pgm.community.motd.MotdFeature;
import dev.pgm.community.mutations.MutationType;
import dev.pgm.community.mutations.feature.MutationFeature;
import dev.pgm.community.network.feature.NetworkFeature;
import dev.pgm.community.network.types.RedisNetworkFeature;
import dev.pgm.community.nick.feature.NickFeature;
import dev.pgm.community.nick.feature.types.SQLNickFeature;
import dev.pgm.community.teleports.TeleportFeature;
import dev.pgm.community.teleports.TeleportFeatureBase;
import dev.pgm.community.users.feature.UsersFeature;
import dev.pgm.community.users.feature.types.SQLUsersFeature;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;

/** Manages all {@link Feature}s of the plugin */
public class FeatureManager {

  private final AssistanceFeature reports;
  private final ModerationFeature moderation;
  private final UsersFeature users;
  private final FriendshipFeature friends;
  private final NetworkFeature network;
  private final NickFeature nick;

  private final TeleportFeature teleports;
  private final InfoCommandsFeature infoCommands;
  private final ChatManagementFeature chat;
  private final MotdFeature motd;
  private final FreezeFeature freeze;
  private final MutationFeature mutation;
  private final BroadcastFeature broadcast;

  public FeatureManager(
      Configuration config,
      Logger logger,
      DatabaseConnection database,
      BukkitCommandManager commands) {
    // Networking
    this.network = new RedisNetworkFeature(config, logger);

    // DB Features
    this.users = new SQLUsersFeature(config, logger, database);
    this.reports = new SQLAssistanceFeature(config, logger, database, users);
    this.moderation = new SQLModerationFeature(config, logger, database, users, network);
    this.friends = new SQLFriendshipFeature(config, logger, database, users);
    this.nick = new SQLNickFeature(config, logger, database, users);
    // TODO: 1. Add support for non-persist database (e.g NoDBUsersFeature)
    // TODO: 2. Support non-sql databases?
    // Ex. FileReportFeature, MongoReportFeature, RedisReportFeature...
    // Not a priority

    // Non-DB Features
    this.teleports = new TeleportFeatureBase(config, logger);
    this.infoCommands = new InfoCommandsFeature(config, logger);
    this.chat = new ChatManagementFeature(config, logger);
    this.motd = new MotdFeature(config, logger);
    this.freeze = new FreezeFeature(config, logger);
    this.mutation = new MutationFeature(config, logger);
    this.broadcast = new BroadcastFeature(config, logger);

    this.registerCommands(commands);
  }

  public AssistanceFeature getReports() {
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

  public FreezeFeature getFreeze() {
    return freeze;
  }

  public MutationFeature getMutations() {
    return mutation;
  }

  public NickFeature getNick() {
    return nick;
  }

  public BroadcastFeature getBroadcast() {
    return broadcast;
  }

  // Register Feature commands and any dependency
  private void registerCommands(BukkitCommandManager commands) {
    // Dependency injection for features
    commands.registerDependency(UsersFeature.class, getUsers());
    commands.registerDependency(AssistanceFeature.class, getReports());
    commands.registerDependency(ModerationFeature.class, getModeration());
    commands.registerDependency(TeleportFeature.class, getTeleports());
    commands.registerDependency(ChatManagementFeature.class, getChatManagement());
    commands.registerDependency(FriendshipFeature.class, getFriendships());
    commands.registerDependency(FreezeFeature.class, getFreeze());
    commands.registerDependency(MutationFeature.class, getMutations());
    commands.registerDependency(BroadcastFeature.class, getBroadcast());
    commands.registerDependency(NickFeature.class, getNick());

    // Custom command completions
    commands
        .getCommandCompletions()
        .registerCompletion(
            "mutes",
            x ->
                getModeration().getOnlineMutes().stream()
                    .map(Player::getName)
                    .collect(Collectors.toSet()));

    commands
        .getCommandCompletions()
        .registerCompletion(
            "addMutations",
            x ->
                Stream.of(MutationType.values())
                    .filter(mt -> !getMutations().hasMutation(mt))
                    .map(MutationType::name)
                    .collect(Collectors.toSet()));
    commands
        .getCommandCompletions()
        .registerCompletion(
            "removeMutations",
            x ->
                Stream.of(MutationType.values())
                    .filter(mt -> getMutations().hasMutation(mt))
                    .map(MutationType::name)
                    .collect(Collectors.toSet()));

    // Feature commands
    registerFeatureCommands(getUsers(), commands);
    registerFeatureCommands(getReports(), commands);
    registerFeatureCommands(getModeration(), commands);
    registerFeatureCommands(getTeleports(), commands);
    registerFeatureCommands(getChatManagement(), commands);
    registerFeatureCommands(getFriendships(), commands);
    registerFeatureCommands(getFreeze(), commands);
    registerFeatureCommands(getMutations(), commands);
    registerFeatureCommands(getBroadcast(), commands);
    registerFeatureCommands(getNick(), commands);
    // TODO: Group calls together and perform upon reload
    // will allow commands to be enabled/disabled with features

    // Other commands
    commands.registerCommand(new CommunityPluginCommand());
    commands.registerCommand(new FlightCommand());
    commands.registerCommand(new StaffCommand());
    commands.registerCommand(new GamemodeCommand());
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
    getFreeze().getConfig().reload(config);
    getMutations().getConfig().reload(config);
    getBroadcast().getConfig().reload(config);
    getNick().getConfig().reload(config);
    // TODO: Look into maybe unregister commands for features that have been disabled
    // commands#unregisterCommand
    // Will need to check isEnabled
  }
}

package dev.pgm.community;

import co.aikar.commands.BukkitCommandManager;
import co.aikar.idb.DB;
import co.aikar.idb.Database;
import co.aikar.idb.DatabaseOptions;
import co.aikar.idb.DatabaseOptions.DatabaseOptionsBuilder;
import co.aikar.idb.PooledDatabaseOptions;
import dev.pgm.community.CommunityConfig.DatabaseType;
import dev.pgm.community.feature.FeatureManager;
import dev.pgm.community.reports.ReportCommand;
import dev.pgm.community.reports.feature.ReportFeature;
import dev.pgm.community.usernames.UsernameDebugCommand;
import dev.pgm.community.usernames.UsernameService;
import dev.pgm.community.usernames.types.CachedUsernameService;
import dev.pgm.community.usernames.types.SQLUsernameService;
import java.io.File;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import tc.oc.pgm.util.chat.Audience;

public class Community extends JavaPlugin {

  // Community Config: contains general config options
  private CommunityConfig config;

  // Command Manager
  private BukkitCommandManager commands;

  // Feature Manager
  private FeatureManager features;

  // Services
  private UsernameService usernames;

  @Override
  public void onEnable() {
    plugin = this;
    this.saveDefaultConfig();
    this.reloadConfig();

    this.config = new CommunityConfig(getConfig());

    this.setupDatabase();

    this.setupServices();

    this.features =
        new FeatureManager(getConfig(), config.getDatabaseType(), getLogger(), usernames);

    this.registerCommands();
  }

  @Override
  public void onDisable() {
    if (DB.getGlobalDatabase() != null) {
      DB.close();
    }
  }

  public void reload() {
    config.reload(getConfig());
    features.reloadConfig();
  }

  private void registerCommands() {
    this.commands = new BukkitCommandManager(this);

    // Dependency Registration
    commands.registerDependency(ReportFeature.class, features.getReports());
    commands.registerDependency(UsernameService.class, usernames);

    // Contexts
    commands
        .getCommandContexts()
        .registerIssuerAwareContext(Audience.class, s -> Audience.get(s.getSender()));

    // Command Registration
    commands.registerCommand(new ReportCommand());

    // DEBUG:
    commands.registerCommand(new UsernameDebugCommand());
  }

  public void registerListener(Listener listener) {
    getServer().getPluginManager().registerEvents(listener, this);
  }

  private static Community plugin;

  public static Community get() {
    return plugin;
  }

  private void setupDatabase() {
    if (config.getDatabaseType() == DatabaseType.NONE) {
      getLogger().info("Database: None selected - Some features will not work as efficiently");
      return;
    }

    DatabaseOptionsBuilder options = DatabaseOptions.builder();

    // TODO: Update if more databases than SQL & MySQL are supported
    if (config.getDatabaseType().equals(DatabaseType.SQL)) {
      String dbName = config.getSqlFileName();
      String pathway =
          getDataFolder().getAbsolutePath() + File.separator + String.format("%s.db", dbName);
      options.sqlite(pathway);
    } else {
      options.mysql(
          config.getDatabaseUsername(),
          config.getDatabasePassword(),
          config.getDatabaseTable(),
          config.getDatabaseAddress());
    }

    Database database =
        PooledDatabaseOptions.builder().options(options.build()).createHikariDatabase();

    DB.setGlobalDatabase(database);
  }

  private void setupServices() {
    this.usernames =
        config.getDatabaseType() != DatabaseType.NONE
            ? new SQLUsernameService(getLogger())
            : new CachedUsernameService(getLogger());
  }

  // REMOVE WHEN NOT IN DEV
  public static void log(String format, Object... objects) {
    Bukkit.getConsoleSender()
        .sendMessage(ChatColor.translateAlternateColorCodes('&', String.format(format, objects)));
  }
}

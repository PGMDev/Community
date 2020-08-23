package dev.pgm.community;

import co.aikar.commands.BukkitCommandManager;
import co.aikar.commands.InvalidCommandArgument;
import dev.pgm.community.commands.CommunityPluginCommand;
import dev.pgm.community.database.DatabaseConnection;
import dev.pgm.community.feature.FeatureManager;
import dev.pgm.community.utils.CommandAudience;
import java.sql.SQLException;
import java.time.Duration;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import tc.oc.pgm.util.bukkit.BukkitUtils;
import tc.oc.pgm.util.text.TextException;
import tc.oc.pgm.util.text.TextParser;

public class Community extends JavaPlugin {

  // Config for general stuff (database)
  private CommunityConfig config;

  // Command Manager
  private BukkitCommandManager commands;

  // Database
  private DatabaseConnection database;

  // Feature Manager
  private FeatureManager features;

  @Override
  public void onEnable() {
    plugin = this;

    this.setupConfig();

    this.setupDatabase();

    this.setupFeatures();
  }

  @Override
  public void onDisable() {
    if (database != null) {
      database.close();
    }

    // TODO: Shutdown other things?
  }

  public void reload() {
    config.reload(getConfig());
    features.reloadConfig();
  }

  private void setupConfig() {
    this.saveDefaultConfig();
    this.reloadConfig();
    this.config = new CommunityConfig(getConfig());
  }

  private void setupCommands() {
    this.commands = new BukkitCommandManager(this);

    // Contexts
    commands
        .getCommandContexts()
        .registerIssuerAwareContext(CommandAudience.class, c -> new CommandAudience(c.getSender()));

    commands
        .getCommandContexts()
        .registerContext(
            Duration.class,
            c -> {
              Duration value = Duration.ZERO;
              String time = c.popFirstArg();
              if (time != null) {
                try {
                  value = TextParser.parseDuration(time);
                } catch (TextException e) {
                  throw new InvalidCommandArgument(
                      time + " is not a valid duration"); // TODO: Translate this
                }
              }
              return value;
            });

    commands.registerCommand(new CommunityPluginCommand());
  }

  public void registerListener(Listener listener) {
    getServer().getPluginManager().registerEvents(listener, this);
  }

  private void setupDatabase() {
    try {
      this.database =
          new DatabaseConnection(config.getDatabaseUri(), config.getMaxDatabaseConnections());
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  private void setupFeatures() {
    this.setupCommands();
    this.features = new FeatureManager(getConfig(), getLogger(), database, commands);
  }

  public String getServerName() {
    return BukkitUtils.colorize(
        config.getServerDisplayName() == null ? "&b&lCommunity" : config.getServerDisplayName());
  }

  // Not the best practice, only use where makes sense
  private static Community plugin;

  public static Community get() {
    return plugin;
  }

  // REMOVE WHEN NOT IN DEV
  public static void log(String format, Object... objects) {
    Bukkit.getConsoleSender()
        .sendMessage(
            ChatColor.translateAlternateColorCodes(
                '&', String.format("&7[&4Community&7]&r " + format, objects)));
  }
}

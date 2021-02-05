package dev.pgm.community;

import co.aikar.commands.BukkitCommandManager;
import co.aikar.commands.InvalidCommandArgument;
import dev.pgm.community.database.DatabaseConnection;
import dev.pgm.community.feature.FeatureManager;
import dev.pgm.community.nick.feature.NickFeature;
import dev.pgm.community.utils.CommandAudience;
import dev.pgm.community.utils.PGMUtils;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Random;
import java.util.stream.Collectors;
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

  private Random random;

  @Override
  public void onEnable() {
    plugin = this;
    random = new Random();

    // If PGM is not enabled on running server, we need this to ensure things work :)
    if (!PGMUtils.isPGMEnabled()) {
      BukkitUtils.PLUGIN.set(this);
    }

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
    this.reloadConfig();
    config.reload(getConfig());
    features.reloadConfig(getConfig());
  }

  private void setupConfig() {
    this.saveDefaultConfig();
    this.reloadConfig();
    this.config = new CommunityConfig(getConfig());
  }

  private void setupCommands() {
    this.commands = new BukkitCommandManager(this);
    commands.registerDependency(Random.class, new Random());

    // Contexts
    commands
        .getCommandContexts()
        .registerIssuerOnlyContext(CommandAudience.class, c -> new CommandAudience(c.getSender()));

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
              return value.abs();
            });

    // Command Completions

    // Use for commands ALL players have access to, allows for vanished players to be hidden from
    // tab-complete
    // Note: use @players if you need ALL players
    commands
        .getCommandCompletions()
        .registerCompletion(
            "visible",
            c -> {
              // TODO: maybe add a config value to allow specific vanish perms to check (if server
              // was not using PGM)
              return Bukkit.getOnlinePlayers().stream()
                  .filter(
                      p ->
                          !p.hasMetadata("isVanished")
                              || c.getPlayer() != null && c.getPlayer().hasPermission("pgm.vanish"))
                  .map(
                      player -> {
                        // Replace nicked user names
                        if (features.getNick() != null) {
                          NickFeature nicks = features.getNick();
                          if (nicks.isNicked(player.getUniqueId())) {
                            return nicks.getOnlineNick(player.getUniqueId());
                          }
                        }
                        return player.getName();
                      })
                  .collect(Collectors.toSet());
            });
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

  public FeatureManager getFeatures() {
    return features;
  }

  // Not the best practice, only use where makes sense
  private static Community plugin;

  public static Community get() {
    return plugin;
  }

  public CommunityConfig getServerConfig() {
    return config;
  }

  public Random getRandom() {
    return random;
  }

  // REMOVE WHEN NOT IN DEV
  public static void log(String format, Object... objects) {
    Bukkit.getConsoleSender()
        .sendMessage(
            ChatColor.translateAlternateColorCodes(
                '&', String.format("&7[&4Community&7]&r " + format, objects)));
  }
}

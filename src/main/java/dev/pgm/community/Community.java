package dev.pgm.community;

import dev.pgm.community.commands.graph.CommunityCommandGraph;
import dev.pgm.community.database.DatabaseConnection;
import dev.pgm.community.events.CommunityEvent;
import dev.pgm.community.feature.FeatureManager;
import dev.pgm.community.utils.PGMUtils;
import fr.minuskube.inv.InventoryManager;
import java.util.Random;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import tc.oc.pgm.util.bukkit.BukkitUtils;

public class Community extends JavaPlugin {

  // Config for general stuff (database)
  private CommunityConfig config;

  // Database
  private DatabaseConnection database;

  // Feature Manager
  private FeatureManager features;

  private InventoryManager inventory;

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
    features.disable();
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

  private void setupDatabase() {
    this.database = new DatabaseConnection(this);
  }

  private void setupCommands() {
    try {
      new CommunityCommandGraph(this);
    } catch (Exception e) {
      e.printStackTrace();
    }

    //    this.commands = new BukkitCommandManager(this);
    //    commands.registerDependency(Random.class, new Random());
    //    commands.registerDependency(Instant.class, "startTime", startTime);
    //    commands.registerDependency(InventoryManager.class, inventory);
    //
    //    // Contexts
    //    commands
    //        .getCommandContexts()
    //        .registerIssuerOnlyContext(CommandAudience.class, c -> new
    // CommandAudience(c.getSender()));
    //
    //    commands
    //        .getCommandContexts()
    //        .registerContext(
    //            Duration.class,
    //            c -> {
    //              Duration value = Duration.ZERO;
    //              String time = c.popFirstArg();
    //              if (time != null) {
    //                try {
    //                  value = TextParser.parseDuration(time);
    //                } catch (TextException e) {
    //                  throw new InvalidCommandArgument(
    //                      time + " is not a valid duration"); // TODO: Translate this
    //                }
    //              }
    //              return value.abs();
    //            });
    //
    //    // Command Completions
    //
    //    // Use for commands ALL players have access to, allows for vanished players to be hidden
    // from
    //    // tab-complete
    //    // Note: use @players if you need ALL players
    //    commands
    //        .getCommandCompletions()
    //        .registerCompletion(
    //            "visible",
    //            c -> {
    //              // TODO: maybe add a config value to allow specific vanish perms to check (if
    // server
    //              // was not using PGM)
    //              return Bukkit.getOnlinePlayers().stream()
    //                  .filter(
    //                      p ->
    //                          !p.hasMetadata("isVanished")
    //                              || c.getPlayer() != null &&
    // c.getPlayer().hasPermission("pgm.vanish"))
    //                  .map(
    //                      player -> {
    //                        // Replace nicked user names
    //                        if (features.getNick() != null) {
    //                          NickFeature nicks = features.getNick();
    //                          if (nicks.isNicked(player.getUniqueId())) {
    //                            return nicks.getOnlineNick(player.getUniqueId());
    //                          }
    //                        }
    //                        return player.getName();
    //                      })
    //                  .collect(Collectors.toSet());
    //            });
  }

  private void setupInventory() {
    this.inventory = new InventoryManager(this);
    this.inventory.init();
  }

  public void registerListener(Listener listener) {
    getServer().getPluginManager().registerEvents(listener, this);
  }

  private void setupFeatures() {
    this.setupInventory();
    this.features = new FeatureManager(getConfig(), getLogger(), database, inventory);
    this.setupCommands();
  }

  public String getServerName() {
    return BukkitUtils.colorize(
        config.getServerDisplayName() == null ? "&b&lCommunity" : config.getServerDisplayName());
  }

  public String getServerId() {
    return config.getServerId();
  }

  public FeatureManager getFeatures() {
    return features;
  }

  public InventoryManager getInventory() {
    return inventory;
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

  public void callEvent(CommunityEvent event) {
    getServer().getPluginManager().callEvent(event);
  }

  // REMOVE WHEN NOT IN DEV
  public static void log(String format, Object... objects) {
    Bukkit.getConsoleSender()
        .sendMessage(
            ChatColor.translateAlternateColorCodes(
                '&', String.format("&7[&4Community&7]&r " + format, objects)));
  }
}

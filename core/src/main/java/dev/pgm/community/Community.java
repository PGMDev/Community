package dev.pgm.community;

import dev.pgm.community.commands.graph.CommunityCommandGraph;
import dev.pgm.community.events.CommunityEvent;
import dev.pgm.community.feature.FeatureManager;
import dev.pgm.community.squads.SquadChannel;
import dev.pgm.community.text.TextTranslations;
import dev.pgm.community.util.Platform;
import dev.pgm.community.utils.PGMUtils;
import dev.pgm.community.utils.WebUtils;
import fr.minuskube.inv.InventoryManager;
import java.util.Random;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import tc.oc.pgm.api.integration.Integration;
import tc.oc.pgm.util.bukkit.BukkitUtils;

public class Community extends JavaPlugin {

  // Config for general stuff (database)
  private CommunityConfig config;

  // Feature Manager
  private FeatureManager features;

  private InventoryManager inventory;

  private Random random;

  @Override
  public void onLoad() {
    Integration.registerChannel(SquadChannel.INSTANCE);
  }

  @Override
  public void onEnable() {
    plugin = this;
    random = new Random();

    // If PGM is not enabled on running server, we need this to ensure things work :)
    if (!PGMUtils.isPGMEnabled()) {
      BukkitUtils.PLUGIN.set(this);
    }

    // Sanity test PGM is running on a supported version before doing any work
    try {
      Platform.init();
      Platform.MANIFEST.onEnable(this);
    } catch (Throwable t) {
      getLogger().log(Level.SEVERE, "Failed to initialize Community platform", t);
      getServer().getPluginManager().disablePlugin(this);
      return;
    }

    this.setupConfig();
    getLogger().info(dev.pgm.community.database.DatabaseExecutor.describeBackend());
    this.setupFeatures();
  }

  @Override
  public void onDisable() {
    if (features != null) features.disable();
    Platform.MANIFEST.onDisable();
    dev.pgm.community.database.DatabaseExecutor.shutdown();
  }

  public void reload() {
    this.reloadConfig();
    config.reload(getConfig());
    features.reloadConfig(getConfig());

    WebUtils.setRandomNameAPI(config.getRandomNameAPIAddress());
    WebUtils.setUsernameAPI(config.getMojangAPIAddress());
  }

  private void setupConfig() {
    this.saveDefaultConfig();
    this.reloadConfig();
    this.config = new CommunityConfig(getConfig());
  }

  private void setupCommands() {
    try {
      new CommunityCommandGraph(this);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void setupInventory() {
    this.inventory = new InventoryManager(this);
    this.inventory.init();
  }

  private void setupTranslations() {
    TextTranslations.load();
  }

  public void registerListener(Listener listener) {
    getServer().getPluginManager().registerEvents(listener, this);
  }

  private void setupFeatures() {
    this.setupTranslations();
    this.setupInventory();
    this.features = new FeatureManager(getConfig(), getLogger(), inventory);
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
    if (Bukkit.isPrimaryThread()) {
      getServer().getPluginManager().callEvent(event);
    } else {
      getServer()
          .getScheduler()
          .runTask(this, () -> getServer().getPluginManager().callEvent(event));
    }
  }

  // REMOVE WHEN NOT IN DEV
  public static void log(String format, Object... objects) {
    Bukkit.getConsoleSender()
        .sendRawMessage(ChatColor.translateAlternateColorCodes(
            '&', String.format("&7[&4Community&7]&r " + format, objects)));
  }
}

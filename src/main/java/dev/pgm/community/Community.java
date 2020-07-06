package dev.pgm.community;

import co.aikar.commands.BukkitCommandManager;
import dev.pgm.community.feature.FeatureManager;
import dev.pgm.community.reports.ReportCommand;
import dev.pgm.community.reports.ReportFeature;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class Community extends JavaPlugin {

  // Command Manager
  private BukkitCommandManager commands;

  // Feature Manager
  private FeatureManager features;

  @Override
  public void onEnable() {
    plugin = this;

    this.saveDefaultConfig();
    this.reloadConfig();

    this.features = new FeatureManager(getConfig());

    this.registerCommands();
  }

  @Override
  public void onDisable() {
    // TODO: save config, shutdown database, etc
  }

  @Override
  public void reloadConfig() {
    super.reloadConfig();
    features.reloadConfig();
  }

  private void registerCommands() {
    this.commands = new BukkitCommandManager(this);

    // Dependency Registration
    commands.registerDependency(ReportFeature.class, features.getReports());

    // Command Registration
    commands.registerCommand(new ReportCommand());
  }

  public void registerListener(Listener listener) {
    getServer().getPluginManager().registerEvents(listener, this);
  }

  private static Community plugin;

  public static Community get() {
    return plugin;
  }
}

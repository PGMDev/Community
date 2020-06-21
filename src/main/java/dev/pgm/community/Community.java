package dev.pgm.community;

import app.ashcon.intake.bukkit.BukkitIntake;
import app.ashcon.intake.bukkit.graph.BasicBukkitCommandGraph;
import app.ashcon.intake.fluent.DispatcherNode;
import dev.pgm.community.reports.ReportCommand;
import dev.pgm.community.reports.ReportManager;
import org.bukkit.plugin.java.JavaPlugin;

public class Community extends JavaPlugin {

  private Config config;
  private BasicBukkitCommandGraph commands;

  // Features
  private ReportManager reportManager;

  @Override
  public void onEnable() {

    this.saveDefaultConfig();
    this.reloadConfig();

    this.config = new Config(getConfig());

    this.setupManagers();
    this.registerCommands();
  }

  private void setupManagers() {
    this.reportManager = new ReportManager(config);
  }

  private void registerCommands() {
    this.commands = new BasicBukkitCommandGraph();

    registerCommand(new ReportCommand(reportManager));

    new BukkitIntake(this, commands).register();
    ;
  }

  private void registerCommand(Object command, String... aliases) {
    DispatcherNode commandNodes = commands.getRootDispatcherNode();
    if (aliases.length > 1) {
      commandNodes.registerNode(aliases);
    }
    commandNodes.registerCommands(command);
  }
}

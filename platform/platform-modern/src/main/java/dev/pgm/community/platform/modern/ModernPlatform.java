package dev.pgm.community.platform.modern;

import static dev.pgm.community.util.Supports.Priority.HIGHEST;
import static dev.pgm.community.util.Supports.Variant.PAPER;

import dev.pgm.community.util.Platform;
import dev.pgm.community.util.Supports;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

@Supports(value = PAPER, minVersion = "1.21.11", priority = HIGHEST)
public class ModernPlatform implements Platform.Manifest {
  private PacketManipulations packetManipulations;

  @Override
  public void onEnable(Plugin plugin) {
    if (!plugin.getServer().getPluginManager().isPluginEnabled("packetevents")) {
      Bukkit.getServer().getPluginManager().disablePlugin(plugin);
      throw new IllegalStateException(
          "PacketEvents is not installed, and is required for Community modern version support");
    }

    packetManipulations = new PacketManipulations();
  }

  @Override
  public void onDisable() {
    if (packetManipulations != null) {
      packetManipulations.unregister();
      packetManipulations = null;
    }
  }
}

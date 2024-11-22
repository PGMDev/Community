package dev.pgm.community.platform.modern;

import static dev.pgm.community.util.Supports.Priority.HIGHEST;
import static dev.pgm.community.util.Supports.Variant.PAPER;

import dev.pgm.community.util.Platform;
import dev.pgm.community.util.Supports;
import org.bukkit.plugin.Plugin;

@Supports(value = PAPER, minVersion = "1.20.6", priority = HIGHEST)
public class ModernPlatform implements Platform.Manifest {
  @Override
  public void onEnable(Plugin plugin) {
    new PacketManipulations(plugin);
  }
}

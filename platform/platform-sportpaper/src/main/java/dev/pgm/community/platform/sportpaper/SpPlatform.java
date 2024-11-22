package dev.pgm.community.platform.sportpaper;

import static dev.pgm.community.util.Supports.Priority.HIGHEST;
import static dev.pgm.community.util.Supports.Variant.SPORTPAPER;

import dev.pgm.community.util.Platform;
import dev.pgm.community.util.Supports;
import org.bukkit.plugin.Plugin;

@Supports(value = SPORTPAPER, priority = HIGHEST)
public class SpPlatform implements Platform.Manifest {
  @Override
  public void onEnable(Plugin plugin) {}
}

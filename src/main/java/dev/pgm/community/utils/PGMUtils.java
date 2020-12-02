package dev.pgm.community.utils;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import tc.oc.pgm.api.PGM;

public class PGMUtils {

  public static boolean isPGMEnabled() {
    Plugin pgmPlugin = Bukkit.getServer().getPluginManager().getPlugin("PGM");
    return pgmPlugin != null && pgmPlugin.isEnabled() && PGM.get() != null;
  }
}

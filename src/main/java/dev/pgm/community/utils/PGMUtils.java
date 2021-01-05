package dev.pgm.community.utils;

import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;

public class PGMUtils {

  public static boolean isPGMEnabled() {
    Plugin pgmPlugin = Bukkit.getServer().getPluginManager().getPlugin("PGM");
    return pgmPlugin != null && pgmPlugin.isEnabled() && PGM.get() != null;
  }

  public static @Nullable Match getMatch() {
    return isPGMEnabled() && PGM.get().getMatchManager().getMatches().hasNext()
        ? PGM.get().getMatchManager().getMatches().next()
        : null;
  }
}

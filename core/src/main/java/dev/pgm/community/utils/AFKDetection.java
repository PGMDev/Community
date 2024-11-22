package dev.pgm.community.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import tc.oc.occ.afk.AFKPlugin;

public class AFKDetection {

  private Plugin afkPlugin;

  public AFKDetection() {
    afkPlugin = Bukkit.getPluginManager().getPlugin("AFK");
  }

  public boolean isAFK(Player player) {
    if (afkPlugin != null && afkPlugin.isEnabled()) {
      return isPlayerAFK(player);
    }
    return false;
  }

  private boolean isPlayerAFK(Player player) {
    return AFKPlugin.get() != null && AFKPlugin.get().getManager().isAFK(player);
  }
}

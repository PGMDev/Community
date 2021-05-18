package dev.pgm.community.utils;

import dev.pgm.community.Community;
import dev.pgm.community.nick.feature.NickFeature;
import dev.pgm.community.vanish.VanishFeature;
import org.bukkit.entity.Player;

public class VisibilityUtils {

  public static boolean isDisguised(Player player) {
    VanishFeature vanish = Community.get().getFeatures().getVanish();
    NickFeature nicks = Community.get().getFeatures().getNick();
    if (vanish != null && nicks != null) {
      return vanish.isVanished(player) || nicks.isNicked(player.getUniqueId());
    }
    return false;
  }
}

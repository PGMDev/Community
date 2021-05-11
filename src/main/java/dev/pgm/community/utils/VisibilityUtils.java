package dev.pgm.community.utils;

import dev.pgm.community.Community;
import org.bukkit.entity.Player;

public class VisibilityUtils {

  public static boolean isDisguised(Player player) {
    return Community.get().getFeatures().getVanish().isVanished(player)
        || Community.get().getFeatures().getNick().isNicked(player.getUniqueId());
  }
}

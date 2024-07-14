package dev.pgm.community.utils;

import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.integration.Integration;

public class VisibilityUtils {

  public static boolean isDisguised(UUID playerId) {
    Player player = Bukkit.getPlayer(playerId);
    return player != null && isDisguised(player);
  }

  public static boolean isDisguised(Player player) {
    return Integration.isVanished(player) || Integration.getNick(player) != null;
  }
}

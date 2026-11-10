package dev.pgm.community.utils;

import dev.pgm.community.CommunityPermissions;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.integration.Integration;
import tc.oc.pgm.util.Players;

public class VisibilityUtils {

  /** Whether the viewer may see through the other player's disguise, to their real name and skin */
  public static boolean shouldRevealDisguise(CommandSender viewer, Player other) {
    return viewer == other
        || Integration.getNick(other) == null
        || Players.isFriend(viewer, other)
        || viewer.hasPermission(Permissions.STAFF)
        || viewer.hasPermission(CommunityPermissions.NICKNAME_VIEW);
  }

  public static boolean isDisguised(UUID playerId) {
    Player player = Bukkit.getPlayer(playerId);
    return player != null && isDisguised(player);
  }

  public static boolean isDisguised(Player player) {
    return Integration.isDisguised(player);
  }
}

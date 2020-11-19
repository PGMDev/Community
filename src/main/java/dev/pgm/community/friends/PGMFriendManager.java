package dev.pgm.community.friends;

import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import tc.oc.pgm.util.friends.FriendProvider;

/**
 * PGMFriendManager - Manager which implements {@link FriendProvider} allowing PGM integeration *
 */
public class PGMFriendManager implements FriendProvider, Listener {

  // Map of online players & a list of all their friends
  private Map<UUID, Set<UUID>> onlineFriendMap = Maps.newHashMap();

  /**
   * Set the friend list for the provided player
   *
   * @param playerId - The player
   * @param friendIds - A set of friend playerIds
   */
  public void setFriends(UUID playerId, Set<UUID> friendIds) {
    this.onlineFriendMap.put(playerId, friendIds);
  }

  @Override
  public boolean areFriends(Player player, Player other) {
    return onlineFriendMap.containsKey(player.getUniqueId())
        ? onlineFriendMap.get(player.getUniqueId()).contains(other.getUniqueId())
        : false;
  }
}

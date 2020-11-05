package dev.pgm.community.friends;

import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import tc.oc.pgm.util.friends.FriendProvider;

public class PGMFriendManager implements FriendProvider, Listener {

  private Map<UUID, Set<UUID>> onlineFriendMap = Maps.newHashMap();

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

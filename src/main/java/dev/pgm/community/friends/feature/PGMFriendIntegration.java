package dev.pgm.community.friends.feature;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.integration.FriendIntegration;

public class PGMFriendIntegration implements FriendIntegration {

  private Map<UUID, Set<UUID>> friends;

  public PGMFriendIntegration() {
    this.friends = Maps.newHashMap();
  }

  public void setFriends(UUID playerId, Set<UUID> friends) {
    this.friends.put(playerId, friends);
  }

  @Override
  public boolean isFriend(Player a, Player b) {
    return friends.getOrDefault(a.getUniqueId(), Sets.newHashSet()).contains(b.getUniqueId());
  }
}

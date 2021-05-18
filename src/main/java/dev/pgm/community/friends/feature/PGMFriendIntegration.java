package dev.pgm.community.friends.feature;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import dev.pgm.community.Community;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.event.FriendStatusChangeEvent;
import tc.oc.pgm.api.integration.FriendIntegration;
import tc.oc.pgm.api.integration.Integration;

public class PGMFriendIntegration implements FriendIntegration {

  private Map<UUID, Set<UUID>> friends;

  public PGMFriendIntegration() {
    this.friends = Maps.newHashMap();
    enable();
  }

  public void enable() {
    Integration.setFriendIntegration(this);
  }

  public void setFriends(UUID playerId, Set<UUID> friends) {
    this.friends.put(playerId, friends);
  }

  @Override
  public boolean isFriend(Player a, Player b) {
    return friends.getOrDefault(a.getUniqueId(), Sets.newHashSet()).contains(b.getUniqueId());
  }

  public void callUpdateEvents(UUID playerId, Set<UUID> friendIds) {
    callUpdateEvent(playerId);
    friendIds.forEach(this::callUpdateEvent);
  }

  private void callUpdateEvent(UUID playerId) {
    Community.get().getServer().getPluginManager().callEvent(new FriendStatusChangeEvent(playerId));
  }
}

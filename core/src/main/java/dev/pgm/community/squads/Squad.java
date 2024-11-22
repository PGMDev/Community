package dev.pgm.community.squads;

import com.google.common.collect.Iterables;
import java.util.*;

public class Squad {
  private UUID leader;
  private final Set<UUID> players;
  private final Set<UUID> invites;

  public Squad(UUID leader) {
    this.leader = leader;
    this.players = new LinkedHashSet<>();
    this.invites = new HashSet<>();
    players.add(leader);
  }

  public UUID getLeader() {
    return leader;
  }

  public Set<UUID> getPlayers() {
    // This is READ ONLY. to modify members use the appropriate methods
    return Collections.unmodifiableSet(players);
  }

  public Set<UUID> getInvites() {
    return Collections.unmodifiableSet(invites);
  }

  public Iterable<UUID> getAllPlayers() {
    // This is READ ONLY. to modify members use the appropriate methods
    return Iterables.concat(getPlayers(), getInvites());
  }

  public boolean removePlayer(UUID player) {
    boolean result = players.remove(player);
    if (result && Objects.equals(leader, player) && !players.isEmpty()) {
      leader = players.iterator().next();
    }
    return result;
  }

  public boolean addPlayer(UUID player) {
    if (leader == null) leader = player;
    return players.add(player);
  }

  public boolean containsPlayer(UUID player) {
    return players.contains(player);
  }

  public boolean containsInvite(UUID player) {
    return invites.contains(player);
  }

  public boolean addInvite(UUID player) {
    return !players.contains(player) && invites.add(player);
  }

  public boolean acceptInvite(UUID player) {
    return invites.remove(player) && addPlayer(player);
  }

  public boolean expireInvite(UUID player) {
    return invites.remove(player);
  }

  public int size() {
    return players.size();
  }

  public int totalSize() {
    return players.size() + invites.size();
  }

  public boolean isEmpty() {
    return players.isEmpty();
  }

  @Override
  public String toString() {
    return "Squad{" + "leader=" + leader + ", players=" + players + '}';
  }
}

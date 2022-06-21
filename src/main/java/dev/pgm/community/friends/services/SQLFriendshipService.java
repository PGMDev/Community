package dev.pgm.community.friends.services;

import co.aikar.idb.DB;
import co.aikar.idb.DbRow;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Sets;
import dev.pgm.community.feature.SQLFeatureBase;
import dev.pgm.community.friends.Friendship;
import dev.pgm.community.friends.Friendship.FriendshipStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SQLFriendshipService extends SQLFeatureBase<Friendship, String>
    implements FriendshipQuery {

  private LoadingCache<UUID, PlayerFriendships> friendshipCache;

  public SQLFriendshipService() {
    super(TABLE_NAME, TABLE_FIELDS);
    this.friendshipCache =
        CacheBuilder.newBuilder()
            .build(
                new CacheLoader<UUID, PlayerFriendships>() {
                  @Override
                  public PlayerFriendships load(UUID key) throws Exception {
                    return new PlayerFriendships(key);
                  }
                });
  }

  @Override
  public void save(Friendship friendship) {
    PlayerFriendships cachedRequester = friendshipCache.getIfPresent(friendship.getRequesterId());
    PlayerFriendships cachedRequested = friendshipCache.getIfPresent(friendship.getRequestedId());

    if (cachedRequester != null) {
      cachedRequester.getFriendships().add(friendship);
    }

    if (cachedRequested != null) {
      cachedRequested.getFriendships().add(friendship);
    }

    DB.executeUpdateAsync(
        INSERT_FRIENDSHIP_QUERY,
        friendship.getFriendshipId().toString(),
        friendship.getRequesterId().toString(),
        friendship.getRequestedId().toString(),
        friendship.getStatus().toString().toUpperCase(),
        friendship.getRequestDate().toEpochMilli(),
        friendship.getLastUpdated().toEpochMilli());
  }

  public void updateFriendshipStatus(Friendship friendship, boolean accept) {
    friendship.setStatus(accept ? FriendshipStatus.ACCEPTED : FriendshipStatus.REJECTED);
    friendship.setLastUpdated(Instant.now());

    PlayerFriendships cachedRequester = friendshipCache.getIfPresent(friendship.getRequesterId());
    PlayerFriendships cachedRequested = friendshipCache.getIfPresent(friendship.getRequestedId());

    if (cachedRequester != null) {
      if (accept) {
        cachedRequester.getFriendships().add(friendship);
      } else {
        cachedRequester.getFriendships().remove(friendship);
      }
    }

    if (cachedRequested != null) {
      if (accept) {
        cachedRequested.getFriendships().add(friendship);
      } else {
        cachedRequested.getFriendships().remove(friendship);
      }
    }

    DB.executeUpdateAsync(
        UPDATE_FRIENDSHIP_QUERY,
        friendship.getStatus().toString().toUpperCase(),
        friendship.getLastUpdated().toEpochMilli(),
        friendship.getFriendshipId().toString());
  }

  @Override // TODO: Query list where TARGET == requester or requested, add methods
  // for Incoming & existing
  public CompletableFuture<List<Friendship>> queryList(String target) {
    UUID playerId = UUID.fromString(target);
    PlayerFriendships playerFriendships = friendshipCache.getUnchecked(playerId);

    if (playerFriendships.isLoaded()) {
      return CompletableFuture.completedFuture(new ArrayList<>(playerFriendships.getFriendships()));
    } else {
      return DB.getResultsAsync(SELECT_FRIENDSHIPS_QUERY, playerId.toString(), playerId.toString())
          .thenApplyAsync(
              results -> {
                if (results != null) {
                  for (DbRow row : results) {
                    String id = row.getString("id");
                    String requester = row.getString("requester");
                    String requested = row.getString("requested");
                    String status = row.getString("status");
                    long requestDate = Long.parseLong(row.getString("requestDate"));
                    long updateDate = Long.parseLong(row.getString("updateDate"));

                    Instant requestInstant = Instant.ofEpochMilli(requestDate);
                    Instant updateInstant = Instant.ofEpochMilli(updateDate);

                    playerFriendships
                        .getFriendships()
                        .add(
                            new Friendship(
                                UUID.fromString(id),
                                UUID.fromString(requester),
                                UUID.fromString(requested),
                                FriendshipStatus.valueOf(status.toUpperCase()),
                                requestInstant,
                                updateInstant));
                  }
                }
                playerFriendships.setLoaded(true);
                return new ArrayList<>(playerFriendships.getFriendships());
              });
    }
  }

  @Override
  public CompletableFuture<Friendship> query(String target) {
    return null; // Use queryList
  }

  private class PlayerFriendships {
    private UUID playerId;
    private Set<Friendship> friendships;
    private boolean loaded;

    public PlayerFriendships(UUID playerId) {
      this.playerId = playerId;
      this.friendships = Sets.newHashSet();
      this.loaded = false;
    }

    public UUID getPlayerId() {
      return playerId;
    }

    public Set<Friendship> getFriendships() {
      return friendships;
    }

    public boolean isLoaded() {
      return loaded;
    }

    public void setLoaded(boolean loaded) {
      this.loaded = loaded;
    }
  }
}

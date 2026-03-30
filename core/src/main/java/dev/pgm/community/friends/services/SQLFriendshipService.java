package dev.pgm.community.friends.services;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Sets;
import dev.pgm.community.database.DatabaseExecutor;
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

  private final LoadingCache<UUID, PlayerFriendships> friendshipCache;

  public SQLFriendshipService() {
    super(TABLE_NAME, TABLE_FIELDS);
    this.friendshipCache =
        CacheBuilder.newBuilder().build(CacheLoader.from(PlayerFriendships::new));
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

    DatabaseExecutor.executeUpdateAsync(
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

    DatabaseExecutor.executeUpdateAsync(
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
      return DatabaseExecutor.queryAsync(
              SELECT_FRIENDSHIPS_QUERY,
              row -> {
                String id = row.getString("id");
                String requester = row.getString("requester");
                String requested = row.getString("requested");
                String status = row.getString("status");
                long requestDate = row.getLong("requestDate");
                long updateDate = row.getLong("updateDate");

                Instant requestInstant = Instant.ofEpochMilli(requestDate);
                Instant updateInstant = Instant.ofEpochMilli(updateDate);

                return new Friendship(
                    UUID.fromString(id),
                    UUID.fromString(requester),
                    UUID.fromString(requested),
                    FriendshipStatus.valueOf(status.toUpperCase()),
                    requestInstant,
                    updateInstant);
              },
              playerId.toString(),
              playerId.toString())
          .thenApplyAsync(results -> {
            if (results != null) {
              playerFriendships.getFriendships().addAll(results);
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

  private static class PlayerFriendships {
    private final UUID playerId;
    private final Set<Friendship> friendships;
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

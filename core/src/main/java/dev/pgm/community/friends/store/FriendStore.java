package dev.pgm.community.friends.store;

import dev.pgm.community.friends.Friendship;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface FriendStore {

  CompletableFuture<Integer> save(Friendship friendship);

  void invalidate(UUID playerId);

  CompletableFuture<Integer> updateFriendshipStatus(Friendship friendship, boolean accept);

  CompletableFuture<List<Friendship>> queryList(String target);

  CompletableFuture<Integer> count();
}

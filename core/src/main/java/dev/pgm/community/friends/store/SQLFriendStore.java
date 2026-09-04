package dev.pgm.community.friends.store;

import dev.pgm.community.friends.Friendship;
import dev.pgm.community.friends.services.SQLFriendshipService;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SQLFriendStore implements FriendStore {

  private final SQLFriendshipService service;

  public SQLFriendStore() {
    this.service = new SQLFriendshipService();
  }

  @Override
  public CompletableFuture<Integer> save(Friendship friendship) {
    return service.saveAsync(friendship);
  }

  @Override
  public CompletableFuture<Integer> updateFriendshipStatus(Friendship friendship, boolean accept) {
    return service.updateFriendshipStatus(friendship, accept);
  }

  @Override
  public void invalidate(UUID playerId) {
    service.invalidate(playerId);
  }

  @Override
  public CompletableFuture<List<Friendship>> queryList(String target) {
    return service.queryList(target);
  }

  @Override
  public CompletableFuture<Integer> count() {
    return service.count();
  }
}

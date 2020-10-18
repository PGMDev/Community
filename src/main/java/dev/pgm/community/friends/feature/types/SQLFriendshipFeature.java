package dev.pgm.community.friends.feature.types;

import dev.pgm.community.Community;
import dev.pgm.community.database.DatabaseConnection;
import dev.pgm.community.friends.FriendRequestStatus;
import dev.pgm.community.friends.Friendship;
import dev.pgm.community.friends.Friendship.FriendshipStatus;
import dev.pgm.community.friends.feature.FriendshipFeature;
import dev.pgm.community.friends.feature.FriendshipFeatureBase;
import dev.pgm.community.friends.services.SQLFriendshipService;
import dev.pgm.community.users.feature.UsersFeature;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.util.chat.Audience;

public class SQLFriendshipFeature extends FriendshipFeatureBase {

  private final SQLFriendshipService service;
  private final UsersFeature users;

  public SQLFriendshipFeature(
      Configuration config, Logger logger, DatabaseConnection connection, UsersFeature users) {
    super(config, logger);
    this.service = new SQLFriendshipService(connection);
    this.users = users;
  }

  @Override
  public CompletableFuture<List<Friendship>> getFriends(UUID playerId) {
    return service
        .queryList(playerId.toString())
        .thenApplyAsync(
            q ->
                q.stream()
                    .filter(fr -> fr.getStatus() == FriendshipStatus.ACCEPTED)
                    .collect(Collectors.toList()));
  }

  @Override
  public CompletableFuture<List<Friendship>> getIncomingRequests(UUID playerId) {
    return service
        .queryList(playerId.toString())
        .thenApplyAsync(
            q ->
                q.stream()
                    .filter(
                        fr ->
                            fr.getRequestedId().equals(playerId)
                                && fr.getStatus() == FriendshipStatus.PENDING)
                    .collect(Collectors.toList()));
  }

  @Override
  public CompletableFuture<FriendRequestStatus> addFriend(UUID sender, UUID target) {
    return hasRequested(sender, target)
        .thenApplyAsync(
            requested -> {
              if (requested.isPresent()) {
                Friendship pending = requested.get();
                // If target has already requested you, just accept the friendship
                if (pending.getRequesterId().equals(target)) {
                  acceptFriendship(pending);
                  return FriendRequestStatus.ACCEPTED_EXISTING;
                }

                return FriendRequestStatus.EXISTING; // Already requested
              }

              Friendship request = new Friendship(sender, target);
              service.save(request);

              if (Bukkit.getPlayer(target) != null) {
                Component senderName = users.renderUsername(Optional.of(sender)).join();
                Component accept = FriendshipFeature.createAcceptButton(sender.toString());
                Component reject = FriendshipFeature.createRejectButton(sender.toString());

                Component requestMsg =
                    TextComponent.builder()
                        .append(senderName)
                        .append(" has requested to be your friend. ")
                        .append(accept)
                        .append(" ")
                        .append(reject)
                        .color(TextColor.GOLD)
                        .build();

                Audience.get(Bukkit.getPlayer(target)).sendMessage(requestMsg);
                // TODO: play sound too?
              }

              return FriendRequestStatus.PENDING;
            });
  }

  @Override
  public void removeFriend(UUID sender, Friendship friendship) {
    friendship.setStatus(FriendshipStatus.REJECTED);
    friendship.setLastUpdated(Instant.now());
    service.updateFriendshipStatus(friendship);
    update(friendship);
  }

  @Override
  public CompletableFuture<Boolean> areFriends(UUID sender, UUID target) {
    return service
        .queryList(sender.toString())
        .thenApplyAsync(
            frs ->
                frs.stream()
                    .anyMatch(
                        fr ->
                            fr.areInvolved(sender, target)
                                && fr.getStatus() == FriendshipStatus.ACCEPTED));
  }

  @Override
  public CompletableFuture<Optional<Friendship>> hasRequested(UUID sender, UUID target) {
    return service
        .queryList(target.toString())
        .thenApplyAsync(
            frs ->
                frs.stream()
                    .filter(
                        fr ->
                            fr.areInvolved(sender, target)
                                && fr.getStatus() == FriendshipStatus.PENDING)
                    .findAny());
  }

  @Override
  public void onPreLogin(AsyncPlayerPreLoginEvent event) {
    updateFriendships(event.getUniqueId());
    Community.log("Prelogin was run and friendships should be set!");
  }

  @Override
  public void onDelayedLogin(PlayerJoinEvent event) {
    getIncomingRequests(event.getPlayer().getUniqueId())
        .thenAcceptAsync(
            requests -> {
              if (!requests.isEmpty()) {
                sendFriendRequestLoginMessage(event.getPlayer(), requests.size());
              }
            });
  }

  @Override
  public void acceptFriendship(Friendship friendship) {
    friendship.setStatus(FriendshipStatus.ACCEPTED);
    friendship.setLastUpdated(Instant.now());
    service.updateFriendshipStatus(friendship);
    update(friendship);
  }

  @Override
  public void rejectFriendship(Friendship friendship) {
    friendship.setStatus(FriendshipStatus.REJECTED);
    friendship.setLastUpdated(Instant.now());
    service.updateFriendshipStatus(friendship);
    update(friendship);
  }

  public void update(Friendship friendship) {
    updateFriendships(friendship.getRequestedId());
    updateFriendships(friendship.getRequesterId());
  }

  @Override
  public void updateFriendships(UUID playerId) {
    if (Bukkit.getPluginManager().isPluginEnabled("PGM")) {
      getFriends(playerId)
          .thenAcceptAsync(
              friends -> {
                Set<UUID> friendIds =
                    friends.stream()
                        .map(f -> f.getOtherPlayer(playerId))
                        .collect(Collectors.toSet());
                // Sends updated friendship status to PGM for hook-in
                PGM.get().getFriendManager().setFriends(playerId, friendIds);
              });
    }
  }

  public CompletableFuture<Integer> count() {
    return service.count();
  }
}

package dev.pgm.community.friends.feature.types;

import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;

import dev.pgm.community.friends.FriendRequestStatus;
import dev.pgm.community.friends.Friendship;
import dev.pgm.community.friends.Friendship.FriendshipStatus;
import dev.pgm.community.friends.feature.FriendshipFeature;
import dev.pgm.community.friends.feature.FriendshipFeatureBase;
import dev.pgm.community.friends.services.SQLFriendshipService;
import dev.pgm.community.users.feature.UsersFeature;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.named.NameStyle;

public class SQLFriendshipFeature extends FriendshipFeatureBase {

  private final SQLFriendshipService service;
  private final UsersFeature users;

  public SQLFriendshipFeature(Configuration config, Logger logger, UsersFeature users) {
    super(config, logger, "Friends (SQL)");
    this.service = new SQLFriendshipService();
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

              // Can't add an existing friend ;)
              if (areFriends(sender, target).join()) {
                return FriendRequestStatus.EXISTING;
              }

              Friendship request = new Friendship(sender, target);
              service.save(request);

              if (Bukkit.getPlayer(target) != null) {
                Player targetPlayer = Bukkit.getPlayer(target);

                Component senderName =
                    users.renderUsername(Optional.of(sender), NameStyle.FANCY).join();
                Component accept = FriendshipFeature.createAcceptButton(sender.toString());
                Component reject = FriendshipFeature.createRejectButton(sender.toString());

                Component requestMsg =
                    text()
                        .append(senderName)
                        .append(text(" has requested to be your friend. "))
                        .append(accept)
                        .append(space())
                        .append(reject)
                        .color(NamedTextColor.GOLD)
                        .build();

                Audience.get(targetPlayer).sendMessage(requestMsg);
                // TODO: play sound too?
              }

              return FriendRequestStatus.PENDING;
            });
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
    service.updateFriendshipStatus(friendship, true);
    update(friendship);
  }

  @Override
  public void rejectFriendship(Friendship friendship) {
    service.updateFriendshipStatus(friendship, false);
    update(friendship);
  }

  public boolean isFriend(UUID sender, UUID target) {
    return integration.isFriend(sender, target);
  }

  public void update(Friendship friendship) {
    updateFriendships(friendship.getRequestedId());
    updateFriendships(friendship.getRequesterId());
  }

  @Override
  public void updateFriendships(UUID playerId) {
    getFriends(playerId)
        .thenAcceptAsync(
            friends -> {
              Set<UUID> friendIds =
                  friends.stream().map(f -> f.getOtherPlayer(playerId)).collect(Collectors.toSet());
              // Sends updated friendship status to PGM for hook-in
              if (integration != null) {
                integration.setFriends(playerId, friendIds);
                integration.callUpdateEvents(playerId, friendIds);
              }
            });
  }

  public CompletableFuture<Integer> count() {
    return service.count();
  }
}

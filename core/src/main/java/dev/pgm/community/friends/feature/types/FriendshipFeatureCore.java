package dev.pgm.community.friends.feature.types;

import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;

import dev.pgm.community.Community;
import dev.pgm.community.feature.FeatureBase;
import dev.pgm.community.friends.FriendRequestStatus;
import dev.pgm.community.friends.Friendship;
import dev.pgm.community.friends.Friendship.FriendshipStatus;
import dev.pgm.community.friends.FriendshipConfig;
import dev.pgm.community.friends.feature.FriendshipFeature;
import dev.pgm.community.friends.feature.PGMFriendIntegration;
import dev.pgm.community.friends.store.FriendStore;
import dev.pgm.community.users.feature.UsersFeature;
import dev.pgm.community.utils.BroadcastUtils;
import dev.pgm.community.utils.PGMUtils;
import dev.pgm.community.utils.Sounds;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.named.NameStyle;

public class FriendshipFeatureCore extends FeatureBase implements FriendshipFeature {

  private final FriendStore store;
  private final UsersFeature users;

  @Nullable
  private PGMFriendIntegration integration;

  public FriendshipFeatureCore(
      Configuration config, Logger logger, UsersFeature users, FriendStore store) {
    super(new FriendshipConfig(config), logger, "Friends");
    this.store = store;
    this.users = users;

    if (getConfig().isEnabled()) {
      enable();
    }
  }

  public FriendshipConfig getFriendshipConfig() {
    return (FriendshipConfig) getConfig();
  }

  @Override
  public void enable() {
    super.enable();
    if (isPGMEnabled()) {
      this.integration = new PGMFriendIntegration();
    }
  }

  @Override
  public void onDelayedLogin(PlayerJoinEvent event) {
    getIncomingRequests(event.getPlayer().getUniqueId()).thenAcceptAsync(requests -> {
      if (!requests.isEmpty()) {
        sendFriendRequestLoginMessage(event.getPlayer(), requests.size());
      }
    });
  }

  @Override
  public void onPreLogin(AsyncPlayerPreLoginEvent event) {
    updateFriendships(event.getUniqueId());
  }

  @Override
  public CompletableFuture<List<Friendship>> getFriends(UUID playerId) {
    return store
        .queryList(playerId.toString())
        .thenApplyAsync(q -> q.stream()
            .filter(fr -> fr.getStatus() == FriendshipStatus.ACCEPTED)
            .collect(Collectors.toList()));
  }

  @Override
  public CompletableFuture<List<Friendship>> getIncomingRequests(UUID playerId) {
    return store
        .queryList(playerId.toString())
        .thenApplyAsync(q -> q.stream()
            .filter(fr ->
                fr.getRequestedId().equals(playerId) && fr.getStatus() == FriendshipStatus.PENDING)
            .collect(Collectors.toList()));
  }

  @Override
  public CompletableFuture<FriendRequestStatus> addFriend(UUID sender, UUID target) {
    return hasRequested(sender, target).thenApplyAsync(requested -> {
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
      store.save(request);

      if (Bukkit.getPlayer(target) != null) {
        Player targetPlayer = Bukkit.getPlayer(target);

        Component senderName =
            users.renderUsername(Optional.of(sender), NameStyle.FANCY).join();
        Component accept = FriendshipFeature.createAcceptButton(sender.toString());
        Component reject = FriendshipFeature.createRejectButton(sender.toString());

        Component requestMsg = text()
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
    return store
        .queryList(sender.toString())
        .thenApplyAsync(frs -> frs.stream()
            .anyMatch(fr ->
                fr.areInvolved(sender, target) && fr.getStatus() == FriendshipStatus.ACCEPTED));
  }

  @Override
  public CompletableFuture<Optional<Friendship>> hasRequested(UUID sender, UUID target) {
    return store
        .queryList(target.toString())
        .thenApplyAsync(frs -> frs.stream()
            .filter(
                fr -> fr.areInvolved(sender, target) && fr.getStatus() == FriendshipStatus.PENDING)
            .findAny());
  }

  @Override
  public void acceptFriendship(Friendship friendship) {
    store.updateFriendshipStatus(friendship, true);
    update(friendship);
  }

  @Override
  public void rejectFriendship(Friendship friendship) {
    store.updateFriendshipStatus(friendship, false);
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
    getFriends(playerId).thenAcceptAsync(friends -> {
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
    return store.count();
  }

  public void sendFriendRequestLoginMessage(Player player, int requestCount) {
    Component requestsMessage = text()
        .append(BroadcastUtils.RIGHT_DIV.color(NamedTextColor.GOLD))
        .append(text(" You have "))
        .append(text(requestCount, NamedTextColor.DARK_AQUA, TextDecoration.BOLD))
        .append(text(" pending friend request" + (requestCount != 1 ? "s " : " ")))
        .append(BroadcastUtils.LEFT_DIV.color(NamedTextColor.GOLD))
        .color(NamedTextColor.DARK_GREEN)
        .hoverEvent(
            HoverEvent.showText(text("Click to view pending friend requests", NamedTextColor.GRAY)))
        .clickEvent(ClickEvent.runCommand("/friend requests"))
        .build();

    Audience.get(player).sendMessage(requestsMessage);
    Audience.get(player).playSound(Sounds.FRIEND_REQUEST_LOGIN);
  }

  @EventHandler
  public void onDelayedPlayerJoin(PlayerJoinEvent event) {
    // Used to send online friend requests notifications AFTER all other login messages have been
    // sent
    Bukkit.getScheduler()
        .scheduleSyncDelayedTask(Community.get(), () -> onDelayedLogin(event), 40L);
  }

  @EventHandler
  public void onAsyncLogin(AsyncPlayerPreLoginEvent event) {
    this.onPreLogin(event);
  }

  private boolean isPGMEnabled() {
    return PGMUtils.isPGMEnabled() && getFriendshipConfig().isIntegrationEnabled();
  }
}

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
import dev.pgm.community.network.feature.NetworkFeature;
import dev.pgm.community.network.subs.types.FriendshipSubscriber;
import dev.pgm.community.network.updates.types.FriendshipUpdate;
import dev.pgm.community.settings.CommunitySetting;
import dev.pgm.community.settings.feature.SettingsFeature;
import dev.pgm.community.users.feature.UsersFeature;
import dev.pgm.community.utils.BroadcastUtils;
import dev.pgm.community.utils.PGMUtils;
import dev.pgm.community.utils.Sounds;
import java.time.Duration;
import java.time.Instant;
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
  private final SettingsFeature settings;
  private final NetworkFeature network;

  @Nullable
  private PGMFriendIntegration integration;

  public FriendshipFeatureCore(
      Configuration config,
      Logger logger,
      UsersFeature users,
      FriendStore store,
      SettingsFeature settings,
      NetworkFeature network) {
    super(new FriendshipConfig(config), logger, "Friends");
    this.store = store;
    this.users = users;
    this.settings = settings;
    this.network = network;

    if (getConfig().isEnabled()) {
      enable();
      network.registerSubscriber(new FriendshipSubscriber(this, network.getNetworkId(), logger));
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
    UUID playerId = event.getPlayer().getUniqueId();
    settings
        .isSettingEnabled(playerId, CommunitySetting.FRIEND_REQUESTS)
        .thenAcceptAsync(accepting -> {
          if (!accepting) return; // No request reminders for players who toggled requests off
          getIncomingRequests(playerId).thenAcceptAsync(requests -> {
            if (!requests.isEmpty()) {
              sendFriendRequestLoginMessage(event.getPlayer(), requests.size());
            }
          });
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
    return hasRequested(sender, target).thenComposeAsync(requested -> {
      if (requested.isPresent()) {
        Friendship pending = requested.get();
        // If target has already requested you, just accept the friendship
        if (pending.getRequesterId().equals(target)) {
          return acceptFriendship(pending)
              .thenApply(ignored -> FriendRequestStatus.ACCEPTED_EXISTING);
        }

        return CompletableFuture.completedFuture(FriendRequestStatus.ALREADY_REQUESTED);
      }

      // Can't add an existing friend ;)
      if (areFriends(sender, target).join()) {
        return CompletableFuture.completedFuture(FriendRequestStatus.ALREADY_FRIENDS);
      }

      // Target has toggled off incoming friend requests
      if (!settings.isSettingEnabled(target, CommunitySetting.FRIEND_REQUESTS).join()) {
        return CompletableFuture.completedFuture(FriendRequestStatus.BLOCKED);
      }

      // Sender's previous request was rejected too recently
      if (isOnRequestCooldown(sender, target)) {
        return CompletableFuture.completedFuture(FriendRequestStatus.COOLDOWN);
      }

      Friendship request = new Friendship(sender, target);
      return store.save(request).thenApply(ignored -> {
        broadcastInvalidation(sender, target);

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
          Audience.get(targetPlayer).playSound(Sounds.FRIEND_REQUEST_LOGIN);
        }

        return FriendRequestStatus.PENDING;
      });
    });
  }

  private boolean isOnRequestCooldown(UUID sender, UUID target) {
    Duration cooldown = getFriendshipConfig().getRequestCooldown();
    if (cooldown == null || cooldown.isZero() || cooldown.isNegative()) return false;

    return store.queryList(sender.toString()).join().stream()
        .anyMatch(fr -> fr.areInvolved(sender, target)
            && fr.getStatus() == FriendshipStatus.REJECTED
            && fr.getRequesterId().equals(sender)
            && fr.getLastUpdated() != null
            && fr.getLastUpdated().plus(cooldown).isAfter(Instant.now()));
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
  public CompletableFuture<Void> acceptFriendship(Friendship friendship) {
    return store.updateFriendshipStatus(friendship, true).thenAccept(ignored -> update(friendship));
  }

  @Override
  public CompletableFuture<Void> rejectFriendship(Friendship friendship) {
    return store
        .updateFriendshipStatus(friendship, false)
        .thenAccept(ignored -> update(friendship));
  }

  public boolean isFriend(UUID sender, UUID target) {
    // Integration is only present when PGM + pgm-integration are enabled
    return integration != null && integration.isFriend(sender, target);
  }

  public void update(Friendship friendship) {
    updateFriendships(friendship.getRequestedId());
    updateFriendships(friendship.getRequesterId());
    broadcastInvalidation(friendship.getRequestedId(), friendship.getRequesterId());
  }

  private void broadcastInvalidation(UUID... playerIds) {
    for (UUID playerId : playerIds) {
      network.sendUpdate(new FriendshipUpdate(playerId));
    }
  }

  @Override
  public void receiveNetworkInvalidation(UUID playerId) {
    store.invalidate(playerId);
    // Reload (and push refreshed friend list to PGM) only if the player is on this server
    if (Bukkit.getPlayer(playerId) != null) {
      updateFriendships(playerId);
    }
  }

  @Override
  public void updateFriendships(UUID playerId) {
    getFriends(playerId).thenAcceptAsync(friends -> {
      Set<UUID> friendIds =
          friends.stream().map(f -> f.getOtherPlayer(playerId)).collect(Collectors.toSet());
      // Sends updated friendship status to PGM for hook-in
      if (integration != null) {
        Bukkit.getScheduler().runTask(Community.get(), () -> {
          integration.setFriends(playerId, friendIds);
          integration.callUpdateEvents(playerId, friendIds);
        });
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

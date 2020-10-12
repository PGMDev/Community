package dev.pgm.community.friends.feature;

import dev.pgm.community.feature.Feature;
import dev.pgm.community.friends.FriendRequestStatus;
import dev.pgm.community.friends.Friendship;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.event.HoverEvent;
import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextDecoration;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public interface FriendshipFeature extends Feature {

  void onPreLogin(AsyncPlayerPreLoginEvent event);

  void onDelayedLogin(PlayerJoinEvent event);

  void removeFriend(UUID sender, Friendship friendship);

  CompletableFuture<List<Friendship>> getFriends(UUID playerId);

  CompletableFuture<List<Friendship>> getIncomingRequests(UUID playerId);

  CompletableFuture<FriendRequestStatus> addFriend(UUID sender, UUID target);

  void acceptFriendship(Friendship friendship);

  void rejectFriendship(Friendship friendship);

  CompletableFuture<Boolean> areFriends(UUID sender, UUID target);

  CompletableFuture<Optional<Friendship>> hasRequested(UUID sender, UUID target);

  void updateFriendships(UUID playerId);

  static Component createAcceptButton(String playerId) {
    return TextComponent.of("\u2714", TextColor.GREEN, TextDecoration.BOLD)
        .clickEvent(ClickEvent.runCommand("/friend accept " + playerId))
        .hoverEvent(HoverEvent.showText(TextComponent.of("Click to accept", TextColor.GREEN)));
  }

  static Component createRejectButton(String playerId) {
    return TextComponent.of("\u2715", TextColor.RED, TextDecoration.BOLD)
        .clickEvent(ClickEvent.runCommand("/friend reject " + playerId))
        .hoverEvent(HoverEvent.showText(TextComponent.of("Click to reject", TextColor.RED)));
  }
}

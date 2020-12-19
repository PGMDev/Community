package dev.pgm.community.friends.feature;

import static net.kyori.adventure.text.Component.text;

import dev.pgm.community.feature.Feature;
import dev.pgm.community.friends.FriendRequestStatus;
import dev.pgm.community.friends.Friendship;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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
    return text("\u2714", NamedTextColor.GREEN, TextDecoration.BOLD)
        .clickEvent(ClickEvent.runCommand("/friend accept " + playerId))
        .hoverEvent(HoverEvent.showText(text("Click to accept", NamedTextColor.GREEN)));
  }

  static Component createRejectButton(String playerId) {
    return text("\u2715", NamedTextColor.RED, TextDecoration.BOLD)
        .clickEvent(ClickEvent.runCommand("/friend reject " + playerId))
        .hoverEvent(HoverEvent.showText(text("Click to reject", NamedTextColor.RED)));
  }
}

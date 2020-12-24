package dev.pgm.community.friends.commands;

import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.friends.Friendship;
import dev.pgm.community.friends.feature.FriendshipFeature;
import dev.pgm.community.users.UserProfile;
import dev.pgm.community.users.feature.UsersFeature;
import dev.pgm.community.utils.BroadcastUtils;
import dev.pgm.community.utils.CommandAudience;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.PlayerComponent;
import tc.oc.pgm.util.text.TemporalComponent;
import tc.oc.pgm.util.text.TextFormatter;
import tc.oc.pgm.util.text.formatting.PaginatedComponentResults;

@CommandAlias("friend|friendship|fs")
@Description("Manage your friend relationships")
@CommandPermission(CommunityPermissions.FRIENDSHIP)
public class FriendshipCommand extends CommunityCommand {

  @Dependency private FriendshipFeature friends;
  @Dependency private UsersFeature users;

  @Default
  @CommandAlias("friends")
  @Subcommand("list")
  public void list(CommandAudience sender, @Default("1") int page) {
    if (sender.isPlayer()) {
      friends
          .getFriends(sender.getPlayer().getUniqueId())
          .thenAcceptAsync(
              frs -> {
                sendFriendList(sender, frs, page);
              });
    }
  }

  @Subcommand("requests|incoming|pending")
  @Description("View a list of your pending friend requests")
  public void requests(CommandAudience sender, @Default("1") int page) {
    if (sender.isPlayer()) {
      friends
          .getIncomingRequests(sender.getPlayer().getUniqueId())
          .thenAcceptAsync(
              requests -> {
                sendRequestsList(sender, requests, page);
              });
    }
  }

  @Subcommand("add|request|a")
  @Syntax("[username | uuid] - Name or uuid of friend to add")
  @Description("Sends a friend request to another player")
  @CommandCompletion("@players")
  public void add(CommandAudience sender, String target) {
    if (sender.isPlayer()) {
      getTarget(target, users)
          .thenAcceptAsync(
              storedId -> {
                if (storedId.isPresent()) {
                  if (sender.getId().equals(storedId)) {
                    sender.sendWarning(text("You may not friend yourself..."));
                    return;
                  }

                  friends
                      .addFriend(sender.getPlayer().getUniqueId(), storedId.get())
                      .thenAcceptAsync(
                          status -> {
                            users
                                .renderUsername(Optional.of(storedId.get()))
                                .thenAcceptAsync(
                                    name -> {
                                      switch (status) {
                                        case ACCEPTED_EXISTING:
                                          sender.sendMessage(
                                              text("You accepted ")
                                                  .append(users.renderUsername(storedId).join())
                                                  .append(text("'s friend request!"))
                                                  .color(NamedTextColor.GREEN));
                                          break;
                                        case EXISTING:
                                          if (friends
                                              .areFriends(
                                                  sender.getPlayer().getUniqueId(), storedId.get())
                                              .join()) {
                                            sender.sendWarning(
                                                text("You are already friends with ").append(name));
                                          } else {
                                            sender.sendWarning(
                                                text("You have already sent a friend request to ")
                                                    .append(name)
                                                    .color(NamedTextColor.GRAY));
                                          }
                                          break;
                                        case PENDING:
                                          sender.sendMessage(
                                              text("Friend request sent to ")
                                                  .append(name)
                                                  .color(NamedTextColor.GRAY));
                                          break;
                                        default:
                                          sender.sendWarning(
                                              text("Could not send a friend request to ")
                                                  .append(name)
                                                  .color(NamedTextColor.GRAY));
                                          break;
                                      }
                                    });
                          });
                } else {
                  sender.sendWarning(formatNotFoundComponent(target));
                }
              });
    }
  }

  @Subcommand("remove|delete|rm")
  @Syntax("[username | uuid] - Name or uuid of friend to remove")
  public void remove(CommandAudience sender, String target) {
    if (sender.isPlayer()) {
      getTarget(target, users)
          .thenAcceptAsync(
              storedId -> {
                if (storedId.isPresent()) {
                  if (sender.getId().equals(storedId)) {
                    sender.sendWarning(text("You may not unfriend yourself..."));
                    return;
                  }

                  UUID targetId = storedId.get();
                  friends
                      .getFriends(sender.getPlayer().getUniqueId())
                      .thenAcceptAsync(
                          friendList -> {
                            Optional<Friendship> existing =
                                friendList.stream().filter(fr -> fr.isInvolved(targetId)).findAny();
                            if (existing.isPresent()) {
                              friends.removeFriend(
                                  sender.getPlayer().getUniqueId(), existing.get());
                              sender.sendMessage(
                                  text("You have removed ")
                                      .append(users.renderUsername(storedId).join())
                                      .append(text(" as a friend"))
                                      .color(NamedTextColor.GRAY));
                            } else {
                              sender.sendWarning(
                                  text("You are not friends with ")
                                      .append(users.renderUsername(storedId).join()));
                            }
                          });
                } else {
                  sender.sendWarning(formatNotFoundComponent(target));
                }
              });
    }
  }

  @Subcommand("accept|acc")
  @Syntax("[username | uuid] - Name or uuid to accept")
  @Description("Accept an incoming friend request")
  public void acceptRequest(CommandAudience sender, String target) {
    if (sender.isPlayer()) {
      getTarget(target, users)
          .thenAcceptAsync(
              storedId -> {
                if (storedId.isPresent()) {
                  List<Friendship> requests =
                      friends.getIncomingRequests(sender.getPlayer().getUniqueId()).join();
                  if (requests.isEmpty()) {
                    sender.sendWarning(text("You have no pending friend requests"));
                    return;
                  }

                  Optional<Friendship> pending =
                      requests.stream()
                          .filter(fr -> fr.getRequesterId().equals(storedId.get()))
                          .findAny();

                  if (pending.isPresent()) {
                    friends.acceptFriendship(pending.get());
                    sender.sendMessage(
                        text("You accepted ")
                            .append(users.renderUsername(storedId).join())
                            .append(text("'s friend request!"))
                            .color(NamedTextColor.GREEN));

                    // Notify online requester
                    Player onlineFriend = Bukkit.getPlayer(storedId.get());
                    if (onlineFriend != null) {
                      Audience.get(onlineFriend)
                          .sendMessage(
                              users
                                  .renderUsername(sender.getId())
                                  .join()
                                  .append(
                                      text(
                                          " has accepted your friend request!",
                                          NamedTextColor.GREEN)));
                    }

                  } else {
                    sender.sendWarning(
                        text("You don't have a pending friend request from ")
                            .append(users.renderUsername(storedId).join())
                            .color(NamedTextColor.GRAY));
                  }
                } else {
                  sender.sendWarning(formatNotFoundComponent(target));
                }
              });
    }
  }

  @Subcommand("reject|deny")
  @Syntax("[username | uuid] - Name or uuid to reject")
  public void rejectRequest(CommandAudience sender, String target) {
    if (sender.isPlayer()) {
      getTarget(target, users)
          .thenAcceptAsync(
              storedId -> {
                if (storedId.isPresent()) {
                  List<Friendship> requests =
                      friends.getIncomingRequests(sender.getPlayer().getUniqueId()).join();
                  if (requests.isEmpty()) {
                    sender.sendWarning(text("You have no pending friend requests"));
                    return;
                  }

                  Optional<Friendship> pending =
                      requests.stream()
                          .filter(fr -> fr.getRequesterId().equals(storedId.get()))
                          .findAny();
                  if (pending.isPresent()) {
                    friends.rejectFriendship(pending.get());
                    sender.sendMessage(
                        text("You have rejected ")
                            .append(users.renderUsername(storedId).join())
                            .append(text("'s friend request"))
                            .color(NamedTextColor.GRAY));
                    return;
                  } else {
                    sender.sendWarning(
                        text("You don't have a pending friend request from ")
                            .append(users.renderUsername(storedId).join())
                            .color(NamedTextColor.GRAY));
                  }
                } else {
                  sender.sendWarning(formatNotFoundComponent(target));
                }
              });
    }
  }

  private void sendRequestsList(CommandAudience audience, List<Friendship> requests, int page) {
    Collections.sort(requests); // Sorted by most recent request

    Component headerResultCount = text(Integer.toString(requests.size()), NamedTextColor.RED);

    int perPage = 9;
    int pages = (requests.size() + perPage - 1) / perPage;
    page = Math.max(1, Math.min(page, pages));

    NamedTextColor featureColor = NamedTextColor.YELLOW;

    Component pageNum =
        translatable(
            "command.simplePageHeader",
            NamedTextColor.GRAY,
            text(Integer.toString(page), featureColor),
            text(Integer.toString(pages), featureColor));

    Component header =
        text("Friend Requests", featureColor)
            .append(
                text(" (")
                    .append(headerResultCount)
                    .append(text(") » "))
                    .append(pageNum)
                    .color(NamedTextColor.GRAY));

    Component formattedHeader =
        TextFormatter.horizontalLineHeading(
            audience.getSender(), header, NamedTextColor.DARK_GREEN);
    new PaginatedComponentResults<Friendship>(formattedHeader, perPage) {

      @Override
      public Component format(Friendship data, int index) {
        // [Name] > [ time since requested ] [buttons to accept/reject]
        Component name =
            getFriendName(data.getOtherPlayer(audience.getPlayer().getUniqueId()), null).join();

        return name.append(space())
            .append(BroadcastUtils.RIGHT_DIV.color(NamedTextColor.GOLD))
            .append(text(" Sent "))
            .append(
                TemporalComponent.relativePastApproximate(data.getRequestDate())
                    .color(NamedTextColor.DARK_AQUA))
            .append(space())
            .append(FriendshipFeature.createAcceptButton(data.getRequesterId().toString()))
            .append(space())
            .append(FriendshipFeature.createRejectButton(data.getRequesterId().toString()))
            .color(NamedTextColor.GRAY);
      }

      @Override
      public Component formatEmpty() {
        // TODO: Translate
        return text("You have no pending friend requests", NamedTextColor.RED);
      }
    }.display(audience.getAudience(), requests, page);
  }

  private void sendFriendList(CommandAudience audience, List<Friendship> friends, int page) {
    Collections.sort(
        friends,
        new Comparator<Friendship>() {
          @Override
          public int compare(Friendship o1, Friendship o2) {
            UUID f1 = o1.getOtherPlayer(audience.getPlayer().getUniqueId());
            UUID f2 = o2.getOtherPlayer(audience.getPlayer().getUniqueId());

            UserProfile friend1 = users.getStoredProfile(f1).join();
            UserProfile friend2 = users.getStoredProfile(f2).join();

            Player online1 = Bukkit.getPlayer(f1);
            Player online2 = Bukkit.getPlayer(f2);

            // Sort online friends before offline friends
            if (online1 != null && online2 == null) {
              return -1;
            } else if (online2 != null && online1 == null) {
              return 1;
            }

            return -friend1.getLastLogin().compareTo(friend2.getLastLogin());
          }
        });

    Component headerResultCount =
        text(Integer.toString(friends.size()), NamedTextColor.LIGHT_PURPLE);

    int perPage = 10;
    int pages = (friends.size() + perPage - 1) / perPage;
    page = Math.max(1, Math.min(page, pages));

    NamedTextColor featureColor = NamedTextColor.DARK_PURPLE;

    Component pageNum =
        translatable(
            "command.simplePageHeader",
            NamedTextColor.GRAY,
            text(Integer.toString(page), featureColor),
            text(Integer.toString(pages), featureColor));

    Component header =
        text("Friends", featureColor)
            .append(text(" (").append(headerResultCount).append(text(") » ")).append(pageNum))
            .color(NamedTextColor.GRAY);

    Component formattedHeader =
        TextFormatter.horizontalLineHeading(
            audience.getSender(), header, NamedTextColor.DARK_GREEN);
    new PaginatedComponentResults<Friendship>(formattedHeader, perPage) {

      @Override
      public Component format(Friendship data, int index) {
        Component name =
            getFriendName(
                    data.getOtherPlayer(audience.getPlayer().getUniqueId()), data.getLastUpdated())
                .join();

        TextComponent.Builder builder =
            text()
                .append(name)
                .append(space())
                .append(BroadcastUtils.RIGHT_DIV.color(NamedTextColor.GOLD))
                .append(
                    renderOnlineStatus(
                            data.getOtherPlayer(audience.getPlayer().getUniqueId()),
                            audience.getSender().hasPermission(CommunityPermissions.STAFF))
                        .join());

        if (data.getLastUpdated() != null) {
          Component hover =
              text("Friends for ", NamedTextColor.GRAY)
                  .append(
                      TemporalComponent.duration(
                              Duration.between(data.getLastUpdated(), Instant.now()))
                          .color(NamedTextColor.AQUA));
          builder.hoverEvent(HoverEvent.showText(hover));
        }

        return builder.build();
      }

      @Override
      public Component formatEmpty() {
        // TODO: Translate
        return text("You have no friends yet... :(", NamedTextColor.RED);
      }
    }.display(audience.getAudience(), friends, page);
  }

  private CompletableFuture<Component> renderOnlineStatus(UUID playerId, boolean staff) {
    return users
        .getStoredProfile(playerId)
        .thenApplyAsync(
            profile -> {
              boolean online = Bukkit.getPlayer(playerId) != null;
              boolean vanished = online && Bukkit.getPlayer(playerId).hasMetadata("isVanished");
              boolean visible = online && (!vanished || staff);

              Component status =
                  (visible
                          ? TemporalComponent.duration(
                                  Duration.between(profile.getLastLogin(), Instant.now()))
                              .build()
                          : TemporalComponent.relativePastApproximate(profile.getLastLogin()))
                      .color(visible ? NamedTextColor.GREEN : NamedTextColor.DARK_GREEN);
              return text(visible ? " Online for " : " Last seen ")
                  .append(status)
                  .color(NamedTextColor.GRAY);
            });
  }

  private CompletableFuture<Component> getFriendName(UUID id, @Nullable Instant friendDate) {
    return users
        .getStoredUsername(id)
        .thenApplyAsync(
            name -> PlayerComponent.player(Bukkit.getPlayer(id), name, NameStyle.FANCY));
  }
}

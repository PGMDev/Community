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
import dev.pgm.community.Community;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.friends.Friendship;
import dev.pgm.community.friends.feature.FriendshipFeature;
import dev.pgm.community.sessions.Session;
import dev.pgm.community.users.UserProfile;
import dev.pgm.community.users.feature.UsersFeature;
import dev.pgm.community.utils.BroadcastUtils;
import dev.pgm.community.utils.CommandAudience;
import dev.pgm.community.utils.VisibilityUtils;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.named.NameStyle;
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
                                .renderUsername(Optional.of(storedId.get()), NameStyle.CONCISE)
                                .thenAcceptAsync(
                                    name -> {
                                      switch (status) {
                                        case ACCEPTED_EXISTING:
                                          sender.sendMessage(
                                              text("You accepted ")
                                                  .append(name)
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
                            users
                                .renderUsername(storedId, NameStyle.CONCISE)
                                .thenAcceptAsync(
                                    name -> {
                                      if (existing.isPresent()) {
                                        friends.rejectFriendship(existing.get());
                                        sender.sendMessage(
                                            text("You have removed ")
                                                .append(name)
                                                .append(text(" as a friend"))
                                                .color(NamedTextColor.GRAY));
                                      } else {
                                        sender.sendWarning(
                                            text("You are not friends with ").append(name));
                                      }
                                    });
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

                  users
                      .renderUsername(storedId, NameStyle.CONCISE)
                      .thenAcceptAsync(
                          name -> {
                            if (pending.isPresent()) {
                              friends.acceptFriendship(pending.get());
                              sender.sendMessage(
                                  text("You accepted ")
                                      .append(name)
                                      .append(text("'s friend request!"))
                                      .color(NamedTextColor.GREEN));

                              // Notify online requester
                              Player onlineFriend = Bukkit.getPlayer(storedId.get());
                              if (onlineFriend != null
                                  && !VisibilityUtils.isDisguised(sender.getPlayer())) {
                                Audience.get(onlineFriend)
                                    .sendMessage(
                                        text()
                                            .append(sender.getStyledName())
                                            .append(
                                                text(
                                                    " has accepted your friend request!",
                                                    NamedTextColor.GREEN)));
                              }

                            } else {
                              sender.sendWarning(
                                  text("You don't have a pending friend request from ")
                                      .append(name)
                                      .color(NamedTextColor.GRAY));
                            }
                          });

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
                  users
                      .renderUsername(storedId, NameStyle.CONCISE)
                      .thenAcceptAsync(
                          name -> {
                            if (pending.isPresent()) {
                              friends.rejectFriendship(pending.get());
                              sender.sendMessage(
                                  text("You have rejected ")
                                      .append(name)
                                      .append(text("'s friend request"))
                                      .color(NamedTextColor.GRAY));
                              return;
                            } else {
                              sender.sendWarning(
                                  text("You don't have a pending friend request from ")
                                      .append(name)
                                      .color(NamedTextColor.GRAY));
                            }
                          });
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
            users
                .renderUsername(
                    data.getOtherPlayer(audience.getPlayer().getUniqueId()), NameStyle.CONCISE)
                .join();

        return text()
            .append(name)
            .append(space())
            .append(BroadcastUtils.RIGHT_DIV.color(NamedTextColor.GOLD))
            .append(text(" Sent "))
            .append(
                TemporalComponent.relativePastApproximate(data.getRequestDate())
                    .color(NamedTextColor.DARK_AQUA))
            .append(space())
            .append(FriendshipFeature.createAcceptButton(data.getRequesterId().toString()))
            .append(space())
            .append(FriendshipFeature.createRejectButton(data.getRequesterId().toString()))
            .color(NamedTextColor.GRAY)
            .build();
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

            boolean isStaff = audience.getSender().hasPermission(CommunityPermissions.STAFF);
            Session session1 = friend1.getLatestSession(!isStaff).join();
            Session session2 = friend2.getLatestSession(!isStaff).join();

            Player online1 = Bukkit.getPlayer(f1);
            Player online2 = Bukkit.getPlayer(f2);

            boolean canSee1 = canSee(online1, audience);
            boolean canSee2 = canSee(online2, audience);

            // Sort online friends before offline friends
            if (canSee1 && !canSee2) {
              return -1;
            } else if (canSee2 && !canSee1) {
              return 1;
            }

            return -session1.getLatestUpdateDate().compareTo(session2.getLatestUpdateDate());
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
            users
                .renderUsername(
                    data.getOtherPlayer(audience.getPlayer().getUniqueId()), NameStyle.CONCISE)
                .join();

        TextComponent.Builder builder =
            text()
                .append(name)
                .append(space())
                .append(BroadcastUtils.RIGHT_DIV.color(NamedTextColor.GOLD))
                .append(
                    renderOnlineStatus(
                            data.getOtherPlayer(audience.getPlayer().getUniqueId()), audience)
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

  private CompletableFuture<Component> renderOnlineStatus(UUID playerId, CommandAudience viewer) {
    boolean staff = viewer.getSender().hasPermission(CommunityPermissions.STAFF);
    CompletableFuture<Component> future = new CompletableFuture<Component>();
    users.findUserWithSession(
        playerId,
        !staff,
        (profile, session) -> {
          boolean online = !session.hasEnded();
          boolean vanished = session.isDisguised();
          boolean visible = online && (!vanished || staff);

          Component status =
              (visible
                      ? TemporalComponent.duration(
                              Duration.between(session.getLatestUpdateDate(), Instant.now()))
                          .build()
                      : TemporalComponent.relativePastApproximate(session.getLatestUpdateDate()))
                  .color(visible ? NamedTextColor.GREEN : NamedTextColor.DARK_GREEN);
          future.complete(
              text(visible ? " Online for " : " Last seen ")
                  .append(status)
                  .append(text(session.isOnThisServer() ? "" : " on "))
                  .append(
                      text(session.isOnThisServer() ? "" : session.getServerName())
                          .color(online ? NamedTextColor.GREEN : NamedTextColor.DARK_GREEN))
                  .color(NamedTextColor.GRAY));
        });

    return future;
  }

  private boolean canSee(Player player, CommandAudience viewer) {
    if (player == null) return false;
    if (!viewer.isPlayer()) return true;
    if (isDisguised(player)) {
      if (Community.get().getFeatures().getNick().isNicked(player.getUniqueId())
          && player.hasPermission(CommunityPermissions.OVERRIDE)) {
        return viewer.hasPermission(CommunityPermissions.OVERRIDE);
      }
      return viewer.hasPermission(CommunityPermissions.STAFF);
    }
    return true;
  }
}

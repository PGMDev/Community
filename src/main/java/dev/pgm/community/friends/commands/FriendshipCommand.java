package dev.pgm.community.friends.commands;

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
import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.event.HoverEvent;
import net.kyori.text.format.TextColor;
import org.bukkit.Bukkit;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.PeriodFormats;
import tc.oc.pgm.util.text.TextFormatter;
import tc.oc.pgm.util.text.formatting.PaginatedComponentResults;
import tc.oc.pgm.util.text.types.PlayerComponent;

@CommandAlias("friend|friendship")
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
                    sender.sendWarning(
                        TextComponent.builder().append("You may not friend yourself...").build());
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
                                              TextComponent.builder()
                                                  .append("You accepted ")
                                                  .append(users.renderUsername(storedId).join())
                                                  .append("'s friend request!")
                                                  .color(TextColor.GREEN)
                                                  .build());
                                          break;
                                        case EXISTING:
                                          if (friends
                                              .areFriends(
                                                  sender.getPlayer().getUniqueId(), storedId.get())
                                              .join()) {
                                            sender.sendWarning(
                                                TextComponent.builder()
                                                    .append("You are already friends with ")
                                                    .append(name)
                                                    .build());
                                          } else {
                                            sender.sendWarning(
                                                TextComponent.builder()
                                                    .append(
                                                        "You have already sent a friend request to ")
                                                    .append(name)
                                                    .color(TextColor.GRAY)
                                                    .build());
                                          }
                                          break;
                                        case PENDING:
                                          sender.sendMessage(
                                              TextComponent.builder()
                                                  .append("Friend request sent to ")
                                                  .append(name)
                                                  .color(TextColor.GRAY)
                                                  .build());
                                          break;
                                        default:
                                          sender.sendWarning(
                                              TextComponent.builder()
                                                  .append("Could not send a friend request to ")
                                                  .append(name)
                                                  .color(TextColor.GRAY)
                                                  .build());
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
                    sender.sendWarning(
                        TextComponent.builder().append("You may not unfriend yourself...").build());
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
                                  TextComponent.builder()
                                      .append("You have removed ")
                                      .append(users.renderUsername(storedId).join())
                                      .append(" as a friend")
                                      .color(TextColor.GRAY)
                                      .build());
                            } else {
                              sender.sendWarning(
                                  TextComponent.builder()
                                      .append("You are not friends with ")
                                      .append(users.renderUsername(storedId).join())
                                      .build());
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
                    sender.sendWarning(TextComponent.of("You have no pending friend requests"));
                    return;
                  }

                  Optional<Friendship> pending =
                      requests.stream()
                          .filter(fr -> fr.getRequesterId().equals(storedId.get()))
                          .findAny();

                  if (pending.isPresent()) {
                    friends.acceptFriendship(pending.get());
                    sender.sendMessage(
                        TextComponent.builder()
                            .append("You accepted ")
                            .append(users.renderUsername(storedId).join())
                            .append("'s friend request!")
                            .color(TextColor.GREEN)
                            .build());
                  } else {
                    sender.sendWarning(
                        TextComponent.builder()
                            .append("You don't have a pending friend request from ")
                            .append(users.renderUsername(storedId).join())
                            .color(TextColor.GRAY)
                            .build());
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
                    sender.sendWarning(TextComponent.of("You have no pending friend requests"));
                    return;
                  }

                  Optional<Friendship> pending =
                      requests.stream()
                          .filter(fr -> fr.getRequesterId().equals(storedId.get()))
                          .findAny();
                  if (pending.isPresent()) {
                    friends.rejectFriendship(pending.get());
                    sender.sendMessage(
                        TextComponent.builder()
                            .append("You have rejected ")
                            .append(users.renderUsername(storedId).join())
                            .append("'s friend request")
                            .color(TextColor.GRAY)
                            .build());
                    return;
                  } else {
                    sender.sendWarning(
                        TextComponent.builder()
                            .append("You don't have a pending friend request from ")
                            .append(users.renderUsername(storedId).join())
                            .color(TextColor.GRAY)
                            .build());
                  }
                } else {
                  sender.sendWarning(formatNotFoundComponent(target));
                }
              });
    }
  }

  private void sendRequestsList(CommandAudience audience, List<Friendship> requests, int page) {
    Collections.sort(requests); // Sorted by most recent request

    Component headerResultCount =
        TextComponent.of(Integer.toString(requests.size()), TextColor.RED);

    int perPage = 7;
    int pages = (requests.size() + perPage - 1) / perPage;
    page = Math.max(1, Math.min(page, pages));

    Component pageNum =
        TranslatableComponent.of(
            "command.simplePageHeader",
            TextColor.GRAY,
            TextComponent.of(Integer.toString(page), TextColor.DARK_GREEN),
            TextComponent.of(Integer.toString(pages), TextColor.DARK_GREEN));

    Component header =
        TextComponent.builder()
            .append("Friend Requests", TextColor.YELLOW)
            .append(
                TextComponent.of(" (")
                    .append(headerResultCount)
                    .append(TextComponent.of(") » ", TextColor.GRAY))
                    .append(pageNum))
            .build();

    Component formattedHeader =
        TextFormatter.horizontalLineHeading(audience.getSender(), header, TextColor.DARK_GREEN);
    new PaginatedComponentResults<Friendship>(formattedHeader, perPage) {

      @Override
      public Component format(Friendship data, int index) {
        // [Name] > [ time since requested ] [buttons to accept/reject]
        Component name =
            getFriendName(data.getOtherPlayer(audience.getPlayer().getUniqueId()), null).join();

        return TextComponent.builder()
            .append(name)
            .append(TextComponent.space())
            .append(BroadcastUtils.RIGHT_DIV.color(TextColor.GOLD))
            .append(" Sent ")
            .append(
                PeriodFormats.relativePastApproximate(data.getRequestDate())
                    .color(TextColor.DARK_AQUA))
            .append(TextComponent.space())
            .append(FriendshipFeature.createAcceptButton(data.getRequesterId().toString()))
            .append(TextComponent.space())
            .append(FriendshipFeature.createRejectButton(data.getRequesterId().toString()))
            .color(TextColor.GRAY)
            .build();
      }

      @Override
      public Component formatEmpty() {
        // TODO: Translate
        return TextComponent.of("You have no pending friend requests", TextColor.RED);
      }
    }.display(audience.getAudience(), requests, pages);
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
            return -friend1.getLastLogin().compareTo(friend2.getLastLogin());
          }
        });

    Component headerResultCount =
        TextComponent.of(Integer.toString(friends.size()), TextColor.LIGHT_PURPLE);

    int perPage = 7;
    int pages = (friends.size() + perPage - 1) / perPage;
    page = Math.max(1, Math.min(page, pages));

    Component pageNum =
        TranslatableComponent.of(
            "command.simplePageHeader",
            TextColor.GRAY,
            TextComponent.of(Integer.toString(page), TextColor.DARK_PURPLE),
            TextComponent.of(Integer.toString(pages), TextColor.DARK_PURPLE));

    Component header =
        TextComponent.builder()
            .append("Friends", TextColor.DARK_PURPLE)
            .append(
                TextComponent.of(" (")
                    .append(headerResultCount)
                    .append(TextComponent.of(") » "))
                    .append(pageNum))
            .color(TextColor.GRAY)
            .build();

    Component formattedHeader =
        TextFormatter.horizontalLineHeading(audience.getSender(), header, TextColor.DARK_GREEN);
    new PaginatedComponentResults<Friendship>(formattedHeader, perPage) {

      @Override
      public Component format(Friendship data, int index) {
        Component name =
            getFriendName(
                    data.getOtherPlayer(audience.getPlayer().getUniqueId()), data.getLastUpdated())
                .join();

        return TextComponent.builder()
            .append(name)
            .append(TextComponent.space())
            .append(BroadcastUtils.RIGHT_DIV.color(TextColor.GOLD))
            .append(
                renderOnlineStatus(
                        data.getOtherPlayer(audience.getPlayer().getUniqueId()),
                        audience.getSender().hasPermission(CommunityPermissions.STAFF))
                    .join())
            .build();
      }

      @Override
      public Component formatEmpty() {
        // TODO: Translate
        return TextComponent.of("You have no friends yet... :(", TextColor.RED);
      }
    }.display(audience.getAudience(), friends, pages);
  }

  private CompletableFuture<Component> renderOnlineStatus(UUID playerId, boolean staff) {
    return users
        .getStoredProfile(playerId)
        .thenApplyAsync(
            profile -> {
              boolean online = Bukkit.getPlayer(playerId) != null;
              boolean vanished = online && Bukkit.getPlayer(playerId).hasMetadata("isVanished");

              Component status =
                  PeriodFormats.relativePastApproximate(profile.getLastLogin())
                      .color(online ? TextColor.GREEN : TextColor.DARK_GREEN);
              return TextComponent.builder()
                  .append(online && (!vanished || staff) ? " Online since " : " Last seen ")
                  .append(status)
                  .color(TextColor.GRAY)
                  .build();
            });
  }

  private CompletableFuture<Component> getFriendName(UUID id, @Nullable Instant friendDate) {
    return users
        .getStoredUsername(id)
        .thenApplyAsync(
            name -> {
              TextComponent.Builder formatted =
                  TextComponent.builder()
                      .append(PlayerComponent.of(Bukkit.getPlayer(id), name, NameStyle.CONCISE));
              if (friendDate != null) {
                Component hover =
                    TextComponent.builder()
                        .append("Friends since ", TextColor.GRAY)
                        .append(
                            PeriodFormats.relativePastApproximate(friendDate).color(TextColor.AQUA))
                        .build();
                formatted.hoverEvent(HoverEvent.showText(hover));
              }
              return formatted.build();
            });
  }
}

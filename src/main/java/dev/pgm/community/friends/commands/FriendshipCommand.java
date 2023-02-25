package dev.pgm.community.friends.commands;

import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.util.text.TemporalComponent.duration;
import static tc.oc.pgm.util.text.TemporalComponent.relativePastApproximate;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Sets;
import dev.pgm.community.Community;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.commands.player.TargetPlayer;
import dev.pgm.community.friends.Friendship;
import dev.pgm.community.friends.feature.FriendshipFeature;
import dev.pgm.community.nick.feature.NickFeature;
import dev.pgm.community.sessions.Session;
import dev.pgm.community.users.UserProfile;
import dev.pgm.community.users.feature.UsersFeature;
import dev.pgm.community.utils.BroadcastUtils;
import dev.pgm.community.utils.CommandAudience;
import dev.pgm.community.utils.PaginatedComponentResults;
import dev.pgm.community.utils.VisibilityUtils;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import tc.oc.pgm.lib.cloud.commandframework.annotations.Argument;
import tc.oc.pgm.lib.cloud.commandframework.annotations.CommandDescription;
import tc.oc.pgm.lib.cloud.commandframework.annotations.CommandMethod;
import tc.oc.pgm.lib.cloud.commandframework.annotations.CommandPermission;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.player.PlayerComponent;
import tc.oc.pgm.util.text.TextFormatter;

@CommandMethod("friend|friendship|fs|friends")
public class FriendshipCommand extends CommunityCommand {

  private final FriendshipFeature friends;
  private final UsersFeature users;
  private final NickFeature nicks;

  public FriendshipCommand() {
    this.friends = Community.get().getFeatures().getFriendships();
    this.users = Community.get().getFeatures().getUsers();
    this.nicks = Community.get().getFeatures().getNick();
  }

  @CommandMethod("[page]")
  @CommandDescription("View a list of your friends")
  @CommandPermission(CommunityPermissions.FRIENDSHIP)
  public void list(
      CommandAudience sender,
      Player player,
      @Argument(value = "page", defaultValue = "1") int page) {
    friends
        .getFriends(sender.getPlayer().getUniqueId())
        .thenAcceptAsync(
            frs -> {
              sendFriendList(sender, frs, page);
            });
  }

  @CommandMethod("requests [page]")
  @CommandDescription("View a list of your pending friend requests")
  @CommandPermission(CommunityPermissions.FRIENDSHIP)
  public void requests(
      CommandAudience sender,
      Player player,
      @Argument(value = "page", defaultValue = "1") int page) {
    friends
        .getIncomingRequests(sender.getPlayer().getUniqueId())
        .thenAcceptAsync(
            requests -> {
              sendRequestsList(sender, requests, page);
            });
  }

  @CommandMethod("add <player>")
  @CommandDescription("Sends a friend request to another player")
  @CommandPermission(CommunityPermissions.FRIENDSHIP)
  public void add(CommandAudience sender, Player player, @Argument("player") TargetPlayer target) {
    // Handle disguised players with fake requests
    Player nicked = nicks.getPlayerFromNick(target.getIdentifier());
    if (nicked != null) {
      FakeRequests fake = fakeRequests.getUnchecked(sender.getId().get());
      String fullName = nicks.getOnlineNick(nicked.getUniqueId());
      Component fancyName = PlayerComponent.player(nicked, NameStyle.FANCY);

      if (fake.hasRequest(fullName)) {
        sender.sendWarning(
            text("You have already sent a friend request to ")
                .append(fancyName)
                .color(NamedTextColor.GRAY));
        return;
      } else {
        fake.addRequest(fullName);
        sender.sendMessage(
            text("Friend request sent to ").append(fancyName).color(NamedTextColor.GRAY));
        return;
      }
    }

    getTarget(target.getIdentifier(), users)
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
                              .renderUsername(Optional.of(storedId.get()), NameStyle.FANCY)
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
                sender.sendWarning(formatNotFoundComponent(target.getIdentifier()));
              }
            });
  }

  @CommandMethod("remove <player>")
  @CommandDescription("Unfriend the input user")
  @CommandPermission(CommunityPermissions.FRIENDSHIP)
  public void remove(
      CommandAudience sender, Player player, @Argument("player") TargetPlayer target) {
    getTarget(target.getIdentifier(), users)
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
                              .renderUsername(storedId, NameStyle.FANCY)
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
                sender.sendWarning(formatNotFoundComponent(target.getIdentifier()));
              }
            });
  }

  @CommandMethod("accept <username>")
  @CommandDescription("Accept an incoming friend request")
  @CommandPermission(CommunityPermissions.FRIENDSHIP)
  public void acceptRequest(
      CommandAudience sender, Player player, @Argument("username") String target) {
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
                    .renderUsername(storedId, NameStyle.FANCY)
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

  @CommandMethod("reject <player>")
  @CommandPermission(CommunityPermissions.FRIENDSHIP)
  public void rejectRequest(
      CommandAudience sender, Player player, @Argument("player") TargetPlayer target) {
    getTarget(target.getIdentifier(), users)
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
                    .renderUsername(storedId, NameStyle.FANCY)
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
                sender.sendWarning(formatNotFoundComponent(target.getIdentifier()));
              }
            });
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
                    data.getOtherPlayer(audience.getPlayer().getUniqueId()), NameStyle.FANCY)
                .join();

        return text()
            .append(name)
            .append(space())
            .append(BroadcastUtils.RIGHT_DIV.color(NamedTextColor.GOLD))
            .append(text(" Sent "))
            .append(relativePastApproximate(data.getRequestDate()).color(NamedTextColor.DARK_AQUA))
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
                    data.getOtherPlayer(audience.getPlayer().getUniqueId()), NameStyle.FANCY)
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
                      duration(Duration.between(data.getLastUpdated(), Instant.now()))
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
                      ? duration(Duration.between(session.getLatestUpdateDate(), Instant.now()))
                      : relativePastApproximate(session.getLatestUpdateDate()))
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

  private LoadingCache<UUID, FakeRequests> fakeRequests =
      CacheBuilder.newBuilder()
          .expireAfterAccess(3, TimeUnit.HOURS)
          .build(
              new CacheLoader<UUID, FakeRequests>() {
                @Override
                public FakeRequests load(UUID key) throws Exception {
                  return new FakeRequests();
                }
              });

  private class FakeRequests {

    private Set<String> nicknames = Sets.newHashSet();

    public boolean hasRequest(String nickname) {
      return nicknames.contains(nickname);
    }

    public void addRequest(String nickname) {
      nicknames.add(nickname);
    }
  }
}

package dev.pgm.community.users.commands;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.util.player.PlayerComponent.player;
import static tc.oc.pgm.util.text.TemporalComponent.duration;
import static tc.oc.pgm.util.text.TemporalComponent.relativePastApproximate;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import dev.pgm.community.Community;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.commands.player.TargetPlayer;
import dev.pgm.community.friends.feature.FriendshipFeature;
import dev.pgm.community.moderation.feature.ModerationFeature;
import dev.pgm.community.users.feature.UsersFeature;
import dev.pgm.community.utils.BroadcastUtils;
import dev.pgm.community.utils.CommandAudience;
import dev.pgm.community.utils.MessageUtils;
import dev.pgm.community.utils.PaginatedComponentResults;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import tc.oc.pgm.lib.cloud.commandframework.annotations.Argument;
import tc.oc.pgm.lib.cloud.commandframework.annotations.CommandDescription;
import tc.oc.pgm.lib.cloud.commandframework.annotations.CommandMethod;
import tc.oc.pgm.lib.cloud.commandframework.annotations.CommandPermission;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.TemporalComponent;
import tc.oc.pgm.util.text.TextFormatter;

public class UserInfoCommands extends CommunityCommand {

  private final UsersFeature users;
  private final ModerationFeature moderation;
  private final FriendshipFeature friends;

  public UserInfoCommands() {
    this.users = Community.get().getFeatures().getUsers();
    this.moderation = Community.get().getFeatures().getModeration();
    this.friends = Community.get().getFeatures().getFriendships();
  }

  @CommandMethod("seen|lastseen|find <target>")
  @CommandDescription("View when a player was last online")
  @CommandPermission(CommunityPermissions.FIND)
  public void seenPlayer(CommandAudience audience, @Argument("target") TargetPlayer target) {
    boolean staff = audience.hasPermission(CommunityPermissions.STAFF);
    boolean findAnyone = audience.hasPermission(CommunityPermissions.FIND_ANYONE);

    users.findUserWithSession(
        target.getIdentifier(),
        !staff,
        (profile, session) -> {
          if (profile == null || session == null) {
            audience.sendWarning(
                findAnyone
                    ? MessageUtils.formatUnseen(target.getIdentifier())
                    : MessageUtils.formatNotFriend(target.getIdentifier()));
            return;
          }

          if (audience.isPlayer()
              && !friends.isFriend(audience.getPlayer().getUniqueId(), profile.getId())
              && !findAnyone) {
            audience.sendWarning(
                text("You are not friends with ")
                    .append(text(profile.getUsername(), NamedTextColor.DARK_AQUA)));
            return;
          }

          boolean online = !session.hasEnded();
          boolean disguised = session.isDisguised();
          boolean visible = online && (!disguised || staff);

          Component lastSeenMsg =
              text()
                  .append(player(profile.getId(), NameStyle.FANCY))
                  .append(
                      text(
                          visible ? " has been online for " : " was last seen ")) // TODO: translate
                  .append(
                      (visible
                              ? duration(
                                  Duration.between(session.getLatestUpdateDate(), Instant.now()))
                              : TemporalComponent.relativePastApproximate(
                                  session.getLatestUpdateDate()))
                          .color(online ? NamedTextColor.GREEN : NamedTextColor.DARK_GREEN))
                  .append(text(session.isOnThisServer() ? "" : " on "))
                  .append(
                      text(session.isOnThisServer() ? "" : session.getServerName())
                          .color(online ? NamedTextColor.GREEN : NamedTextColor.DARK_GREEN))
                  .color(NamedTextColor.GRAY)
                  .build();
          audience.sendMessage(lastSeenMsg);
        });
  }

  @CommandMethod("alts|alternateaccounts [target]")
  @CommandDescription("View a list of alternate accounts of a player")
  @CommandPermission(CommunityPermissions.LOOKUP_OTHERS)
  public void viewAlts(CommandAudience audience, @Argument("target") TargetPlayer target) {
    if (target == null) {
      showOnlineAlts(audience, 1);
      showBannedAlts(audience, 1);
      return;
    }

    users
        .getStoredProfile(target.getIdentifier())
        .thenAcceptAsync(
            profile -> {
              if (profile == null) {
                audience.sendWarning(MessageUtils.formatUnseen(target.getIdentifier()));
                return;
              }

              Set<UUID> alts = users.getAlternateAccounts(profile.getId()).join();

              Component targetPlayer =
                  users.renderUsername(profile.getId(), NameStyle.COLOR).join();
              if (alts.isEmpty()) {
                audience.sendWarning(
                    translatable("moderation.alts.noAlts", NamedTextColor.GRAY, targetPlayer));
                return;
              }

              Set<Component> altNames =
                  alts.stream()
                      .map(
                          altId -> {
                            Component name = users.renderUsername(altId, NameStyle.COLOR).join();

                            return text()
                                .append(name)
                                .clickEvent(ClickEvent.runCommand("/l " + altId.toString()))
                                .hoverEvent(
                                    HoverEvent.showText(
                                        text(
                                                "Click to view punishment history of ",
                                                NamedTextColor.GRAY)
                                            .append(name)))
                                .build();
                          })
                      .collect(Collectors.toSet());

              Component numberOfAlts =
                  text(alts.size(), NamedTextColor.YELLOW, TextDecoration.BOLD);

              Component altNameList =
                  text()
                      .append(targetPlayer)
                      .append(text(" has "))
                      .append(numberOfAlts)
                      .append(text(" known alternate account"))
                      .append(text(alts.size() != 1 ? "s" : ""))
                      .append(text(": "))
                      .append(TextFormatter.list(altNames, NamedTextColor.GRAY))
                      .color(NamedTextColor.GRAY)
                      .build();

              List<Component> altsWithBans =
                  alts.stream()
                      .filter(altId -> moderation.isBanned(altId.toString()).join())
                      .map(
                          altId -> {
                            Component name = users.renderUsername(altId, NameStyle.FANCY).join();

                            return text()
                                .append(name)
                                .clickEvent(ClickEvent.runCommand("/l " + altId))
                                .hoverEvent(
                                    HoverEvent.showText(
                                        text(
                                                "Click to view punishment history of ",
                                                NamedTextColor.GRAY)
                                            .append(name)))
                                .build();
                          })
                      .collect(Collectors.toList());

              Component numberOfBannedAlts =
                  text(altsWithBans.size(), NamedTextColor.YELLOW, TextDecoration.BOLD);

              Component altBans =
                  text()
                      .append(numberOfBannedAlts)
                      .append(text(" of these accounts are currently banned: "))
                      .append(TextFormatter.list(altsWithBans, NamedTextColor.GRAY))
                      .color(NamedTextColor.GRAY)
                      .build();
              audience.sendMessage(altNameList);
              if (altsWithBans.size() > 0) {
                audience.sendMessage(altBans);
              }
            });
  }

  @CommandMethod("profile|user <target> [all]")
  @CommandDescription("View account info for a player")
  @CommandPermission(CommunityPermissions.LOOKUP_OTHERS)
  public void viewUserProfile(
      CommandAudience audience,
      @Argument("target") TargetPlayer target,
      @Argument(value = "all", defaultValue = "false") boolean viewAll) {
    users.findUserWithSession(
        target.getIdentifier(),
        false,
        (profile, session) -> {
          if (profile == null || session == null) {
            audience.sendWarning(MessageUtils.formatUnseen(target.getIdentifier()));
            return;
          }

          Component header =
              text("Account Info", NamedTextColor.RED)
                  .append(text(" - ", NamedTextColor.GRAY))
                  .append(player(profile.getId(), NameStyle.FANCY));

          Component uuid =
              formatInfoField("UUID", text(profile.getId().toString(), NamedTextColor.YELLOW));
          Component firstLogin =
              formatInfoField("First Login", formatDateWithHover(profile.getFirstLogin()));
          Component lastLogin =
              formatInfoField("Last Login", formatDateWithHover(session.getLatestUpdateDate()));
          Component joinCount =
              formatInfoField(
                  "Join Count", text(profile.getJoinCount(), NamedTextColor.LIGHT_PURPLE));

          Component lastServer =
              formatInfoField("Last Server", text(session.getServerName(), NamedTextColor.AQUA));

          Component knownIPs = formatInfoField("Known IPs", empty());

          audience.sendMessage(
              TextFormatter.horizontalLineHeading(
                  audience.getSender(), header, NamedTextColor.DARK_GRAY));

          audience.sendMessage(uuid);
          audience.sendMessage(firstLogin);
          audience.sendMessage(lastLogin);
          audience.sendMessage(joinCount);
          audience.sendMessage(lastServer);

          if (audience.getSender().hasPermission(CommunityPermissions.RESTRICTED)) {
            users
                .getLatestAddress(profile.getId())
                .thenAcceptAsync(
                    latest -> {
                      final String lastIpFieldName = "Latest IP";

                      if (latest == null) {
                        audience.sendMessage(
                            formatInfoField(
                                lastIpFieldName,
                                text("Unavailable", NamedTextColor.RED)
                                    .hoverEvent(
                                        HoverEvent.showText(
                                            text(
                                                "No IP info found (user has not logged in recently)",
                                                NamedTextColor.RED)))));
                      } else {
                        audience.sendMessage(
                            formatInfoField(
                                lastIpFieldName,
                                text()
                                    .append(text(latest.getAddress(), NamedTextColor.AQUA))
                                    .append(text(" ("))
                                    .append(relativePastApproximate(latest.getDate()))
                                    .append(text(")"))
                                    .color(NamedTextColor.GRAY)
                                    .build()));
                      }
                    });

            users
                .getKnownIPs(profile.getId())
                .thenAcceptAsync(
                    ips -> {
                      if (ips.size() > 1) {
                        final int MAX_VIEWABLE = 6;
                        List<String> viewableIps = Lists.newArrayList(ips);
                        if (!viewAll) {
                          viewableIps = viewableIps.subList(0, Math.min(ips.size(), MAX_VIEWABLE));
                        }
                        audience.sendMessage(
                            knownIPs.append(text("(" + ips.size() + ")", NamedTextColor.GRAY)));
                        audience.sendMessage(
                            formatListItems(
                                viewableIps.stream()
                                    .map(
                                        ip ->
                                            text()
                                                .append(text("     - ", NamedTextColor.YELLOW))
                                                .append(text(ip, NamedTextColor.DARK_AQUA))
                                                .build())
                                    .collect(Collectors.toList())));
                        if (ips.size() > MAX_VIEWABLE && !viewAll) {
                          audience.sendMessage(
                              text()
                                  .append(
                                      text(
                                          ips.size() - MAX_VIEWABLE,
                                          NamedTextColor.YELLOW,
                                          TextDecoration.BOLD))
                                  .append(text(" Additional IPs found! ", NamedTextColor.DARK_AQUA))
                                  .append(text("[", NamedTextColor.GRAY))
                                  .append(text("View All", NamedTextColor.BLUE))
                                  .append(text("]", NamedTextColor.GRAY))
                                  .clickEvent(ClickEvent.runCommand("/profile " + target + " true"))
                                  .hoverEvent(
                                      HoverEvent.showText(
                                          text("Click to view all ips", NamedTextColor.GRAY)))
                                  .build());
                        }
                      }
                    });
          }
        });
  }

  private Component formatDateWithHover(Instant pastDate) {
    DateTimeFormatter timeFormat = DateTimeFormatter.ISO_INSTANT;
    return text()
        .append(
            TemporalComponent.relativePastApproximate(pastDate).color(NamedTextColor.DARK_GREEN))
        .hoverEvent(HoverEvent.showText(text(timeFormat.format(pastDate), NamedTextColor.AQUA)))
        .build();
  }

  private Component formatListItems(Collection<Component> components) {
    return Component.join(newline(), components);
  }

  private Component formatInfoField(String field, Component value) {
    return text()
        .append(BroadcastUtils.BROADCAST_DIV)
        .append(text(field, NamedTextColor.GOLD, TextDecoration.BOLD))
        .append(text(": ", NamedTextColor.WHITE))
        .append(value)
        .build();
  }

  private void showOnlineAlts(CommandAudience audience, int page) {
    Set<Component> altAccounts = Sets.newHashSet();
    Set<UUID> accountedFor = Sets.newHashSet();

    for (Player player : Bukkit.getOnlinePlayers()) {
      Set<UUID> alts = getOnlineAltAccounts(player);

      if (alts.isEmpty() || accountedFor.contains(player.getUniqueId())) {
        continue;
      } else {
        altAccounts.add(formatAltAccountList(player, alts));
        accountedFor.add(player.getUniqueId());
        accountedFor.addAll(alts);
      }
    }

    sendAltList(audience, page, altAccounts, false);
  }

  private void showBannedAlts(CommandAudience audience, int page) {
    CompletableFuture.runAsync(
        () -> {
          Set<Component> altAccounts = Sets.newHashSet();
          Set<UUID> accountedFor = Sets.newHashSet();

          for (Player player : Bukkit.getOnlinePlayers()) {
            Set<UUID> bannedAlts =
                users.getAlternateAccounts(player.getUniqueId()).join().stream()
                    .filter(altId -> moderation.isBanned(altId.toString()).join())
                    .collect(Collectors.toSet());

            if (!bannedAlts.isEmpty() && !accountedFor.contains(player.getUniqueId())) {
              altAccounts.add(formatAltAccountList(player, bannedAlts));
              accountedFor.add(player.getUniqueId());
              accountedFor.addAll(bannedAlts);
            }
          }

          sendAltList(audience, page, altAccounts, true);
        });
  }

  private void sendAltList(
      CommandAudience audience, int page, Set<Component> altAccounts, boolean banned) {
    int perPage = Math.max(15, altAccounts.size());
    int pages = (altAccounts.size() + perPage - 1) / perPage;

    Component pageHeader =
        translatable(
            "command.pageHeader",
            NamedTextColor.GRAY,
            text(Integer.toString(page), NamedTextColor.DARK_AQUA),
            text(Integer.toString(pages), NamedTextColor.DARK_AQUA));

    Component headerText =
        translatable(
            banned ? "Banned Alt-Accounts" : "moderation.alts.header", NamedTextColor.DARK_AQUA);

    Component header =
        text()
            .append(headerText)
            .append(text(" (", NamedTextColor.GRAY))
            .append(text(Integer.toString(altAccounts.size()), NamedTextColor.DARK_AQUA))
            .append(text(")", NamedTextColor.GRAY))
            .append(text(" » ", NamedTextColor.GOLD))
            .append(pageHeader)
            .build();

    Component formattedHeader =
        TextFormatter.horizontalLineHeading(audience.getSender(), header, NamedTextColor.BLUE);

    new PaginatedComponentResults<Component>(formattedHeader, perPage) {
      @Override
      public Component format(Component data, int index) {
        return data;
      }

      @Override
      public Component formatEmpty() {
        // TODO: Translate
        return text(
            "No" + (banned ? " banned" : "") + " alternate accounts found", NamedTextColor.RED);
      }
    }.display(audience.getAudience(), altAccounts, page);
  }

  private Component formatAltAccountList(Player target, Set<UUID> alts) {
    Component names =
        Component.join(
            text(", ", NamedTextColor.GRAY),
            alts.stream()
                .map(pl -> users.renderUsername(pl, NameStyle.FANCY).join())
                .collect(Collectors.toSet()));
    Component size = text(Integer.toString(alts.size()), NamedTextColor.YELLOW);

    return text("[", NamedTextColor.GOLD)
        .append(player(target, NameStyle.FANCY))
        .append(text("] ", NamedTextColor.GOLD))
        .append(text("(", NamedTextColor.GRAY))
        .append(size)
        .append(text("): ", NamedTextColor.GRAY))
        .append(names);
  }

  private Set<UUID> getOnlineAltAccounts(Player target) {
    Set<UUID> sameIPs = Sets.newHashSet();
    String address = target.getAddress().getAddress().getHostAddress();

    for (Player other : Bukkit.getOnlinePlayers()) {
      if (other.getAddress().getAddress().getHostAddress().equals(address)
          && !other.equals(target)) {
        sameIPs.add(other.getUniqueId());
      }
    }

    return sameIPs;
  }
}

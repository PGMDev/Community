package dev.pgm.community.users.commands;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Syntax;
import com.google.common.collect.Sets;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.moderation.feature.ModerationFeature;
import dev.pgm.community.users.feature.UsersFeature;
import dev.pgm.community.utils.BroadcastUtils;
import dev.pgm.community.utils.CommandAudience;
import dev.pgm.community.utils.MessageUtils;
import dev.pgm.community.utils.WebUtils;
import dev.pgm.community.utils.WebUtils.NameEntry;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.PlayerComponent;
import tc.oc.pgm.util.text.TemporalComponent;
import tc.oc.pgm.util.text.TextFormatter;
import tc.oc.pgm.util.text.formatting.PaginatedComponentResults;

public class UserInfoCommands extends CommunityCommand {

  @Dependency private UsersFeature users;
  @Dependency private ModerationFeature moderation;

  @CommandAlias("usernamehistory|uh")
  @Description("View the name history of a user")
  @Syntax("[player]")
  @CommandCompletion("@players")
  @CommandPermission(CommunityPermissions.LOOKUP_OTHERS)
  public void usernameHistory(CommandAudience audience, String target) {
    WebUtils.getUsernameHistory(target)
        .thenAcceptAsync(
            history -> {
              Component username = text(history.getCurrentName(), NamedTextColor.AQUA);

              if (history.getHistory().isEmpty()) {
                audience.sendWarning(
                    text()
                        .append(username)
                        .append(text(" has never changed their username.", NamedTextColor.GRAY))
                        .build());
                return;
              }

              Component headerText =
                  text()
                      .append(text("Username History", NamedTextColor.YELLOW))
                      .append(text(" - ", NamedTextColor.GRAY))
                      .append(username)
                      .build();
              audience.sendMessage(
                  TextFormatter.horizontalLineHeading(
                      audience.getSender(), headerText, NamedTextColor.GRAY));

              int i = 1;
              for (NameEntry name : history.getHistory()) {
                audience.sendMessage(
                    text()
                        .append(text(i++ + ". ", NamedTextColor.WHITE))
                        .append(text(name.getUsername(), NamedTextColor.YELLOW))
                        .append(BroadcastUtils.BROADCAST_DIV)
                        .append(
                            TemporalComponent.relativePastApproximate(name.getDateChanged())
                                .color(NamedTextColor.DARK_AQUA))
                        .build());
              }
            });
  }

  @CommandAlias("seen|lastseen|find")
  @Description("View when a player was last online")
  @Syntax("[player]")
  @CommandCompletion("@players")
  @CommandPermission(CommunityPermissions.FIND)
  public void seenPlayer(CommandAudience audience, String target) {
    boolean staff = audience.getSender().hasPermission(CommunityPermissions.STAFF);

    users.findUserWithSession(
        target,
        !staff,
        (profile, session) -> {
          if (profile == null || session == null) {
            audience.sendWarning(MessageUtils.formatUnseen(target));
            return;
          }

          boolean online = !session.hasEnded();
          boolean disguised = session.isDisguised();
          boolean visible = online && (!disguised || staff);

          Component lastSeenMsg =
              text()
                  .append(
                      PlayerComponent.player(
                          profile.getId(), profile.getUsername(), NameStyle.FANCY))
                  .append(
                      text(
                          visible ? " has been online for " : " was last seen ")) // TODO: translate
                  .append(
                      (visible
                              ? TemporalComponent.duration(
                                      Duration.between(
                                          session.getLatestUpdateDate(), Instant.now()))
                                  .build()
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

  @CommandAlias("alts|alternateaccounts")
  @Description("View a list of alternate accounts of a player")
  @CommandCompletion("@players")
  @Syntax("[target]")
  @CommandPermission(CommunityPermissions.LOOKUP_OTHERS)
  public void viewAlts(CommandAudience audience, @Optional String target) {
    if (target == null) {
      showOnlineAlts(audience, 1);
      showBannedAlts(audience, 1);
      return;
    }

    users
        .getStoredProfile(target)
        .thenAccept(
            profile -> {
              if (profile == null) {
                audience.sendWarning(MessageUtils.formatUnseen(target));
                return;
              }

              users
                  .getAlternateAccounts(profile.getId())
                  .thenAcceptAsync(
                      alts -> {
                        Component targetPlayer =
                            PlayerComponent.player(
                                profile.getId(), profile.getUsername(), NameStyle.FANCY);
                        if (alts.isEmpty()) {
                          audience.sendWarning(
                              translatable(
                                  "moderation.alts.noAlts", NamedTextColor.GRAY, targetPlayer));
                          return;
                        }

                        Set<Component> altNames =
                            alts.stream()
                                .map(
                                    altId -> {
                                      Component name =
                                          PlayerComponent.player(
                                              altId,
                                              users.getStoredUsername(altId).join(),
                                              NameStyle.FANCY);

                                      return text()
                                          .append(name)
                                          .clickEvent(
                                              ClickEvent.runCommand("/l " + altId.toString()))
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
                                      Component name =
                                          PlayerComponent.player(
                                              altId,
                                              users.getStoredUsername(altId).join(),
                                              NameStyle.FANCY);

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

                        Component altBans =
                            text()
                                .append(numberOfAlts)
                                .append(text(" of these accounts are currently banned: "))
                                .append(TextFormatter.list(altsWithBans, NamedTextColor.GRAY))
                                .color(NamedTextColor.GRAY)
                                .build();

                        audience.sendMessage(altNameList);
                        audience.sendMessage(altBans);
                      });
            });
  }

  @CommandAlias("profile|user")
  @Description("View account info for a player")
  @Syntax("(name | uuid)")
  @CommandCompletion("@players")
  @CommandPermission(CommunityPermissions.LOOKUP_OTHERS)
  public void viewUserProfile(CommandAudience audience, String target) {
    users.findUserWithSession(
        target,
        false,
        (profile, session) -> {
          if (profile == null || session == null) {
            audience.sendWarning(MessageUtils.formatUnseen(target));
            return;
          }

          Component header =
              text("Account Info", NamedTextColor.RED)
                  .append(text(" - ", NamedTextColor.GRAY))
                  .append(
                      PlayerComponent.player(
                          profile.getId(), profile.getUsername(), NameStyle.CONCISE));

          Component uuid =
              formatInfoField("UUID", text(profile.getId().toString(), NamedTextColor.YELLOW));
          Component firstLogin =
              formatInfoField("First Login", formatDateWithHover(profile.getFirstLogin()));
          Component lastLogin =
              formatInfoField("Last Login", formatDateWithHover(session.getLatestUpdateDate()));
          Component joinCount =
              formatInfoField("Join Count", text(profile.getJoinCount(), NamedTextColor.YELLOW));

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
                .getKnownIPs(profile.getId())
                .thenAccept(
                    ips -> {
                      audience.sendMessage(knownIPs);
                      audience.sendMessage(
                          formatListItems(
                              ips.stream()
                                  .map(
                                      ip ->
                                          text()
                                              .append(text("      - ", NamedTextColor.YELLOW))
                                              .append(text(ip, NamedTextColor.DARK_AQUA))
                                              .build())
                                  .collect(Collectors.toList())));
                    });
          }
        });
  }

  private Component formatDateWithHover(Instant pastDate) {
    DateTimeFormatter timeFormat = DateTimeFormatter.ISO_INSTANT;
    return text()
        .append(TemporalComponent.relativePastApproximate(pastDate))
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
        return text("No alternate accounts found", NamedTextColor.RED);
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
        .append(PlayerComponent.player(target, NameStyle.FANCY))
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

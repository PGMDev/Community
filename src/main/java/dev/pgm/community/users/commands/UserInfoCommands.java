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
import dev.pgm.community.users.feature.UsersFeature;
import dev.pgm.community.utils.BroadcastUtils;
import dev.pgm.community.utils.CommandAudience;
import dev.pgm.community.utils.MessageUtils;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.text.PlayerComponent;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.TemporalComponent;
import tc.oc.pgm.util.text.TextFormatter;
import tc.oc.pgm.util.text.formatting.PaginatedComponentResults;

public class UserInfoCommands extends CommunityCommand {

  @Dependency private UsersFeature users;

  @CommandAlias("seen|lastseen")
  @Description("View when a player was last online")
  @Syntax("[player]")
  @CommandCompletion("@players")
  @CommandPermission(CommunityPermissions.LOOKUP)
  public void seenPlayer(CommandAudience audience, String target) {
    users
        .getStoredProfile(target)
        .thenAcceptAsync(
            profile -> {
              if (profile == null) {
                audience.sendWarning(MessageUtils.formatUnseen(target));
                return;
              }

              boolean online = Bukkit.getPlayer(profile.getId()) != null;
              boolean vanished =
                  online && Bukkit.getPlayer(profile.getId()).hasMetadata("isVanished");
              boolean staff = audience.getSender().hasPermission(CommunityPermissions.STAFF);
              boolean visible = online && (!vanished || staff);
              NamedTextColor color = (online ? NamedTextColor.GREEN : NamedTextColor.DARK_GREEN);

              Component lastSeenMsg =
                  text()
                      .append(
                          PlayerComponent.player(
                              profile.getId(), profile.getUsername(), NameStyle.FANCY))
                      .append(
                          text(
                              visible
                                  ? " has been online for "
                                  : " was last seen ")) // TODO: translate
                      .append(
                          (visible
                              ? TemporalComponent.duration(
                                      Duration.between(profile.getLastLogin(), Instant.now()),
                                      color)
                                  .build()
                              : TemporalComponent.relativePastApproximate(profile.getLastLogin())
                                  .color(color)))
                      .color(NamedTextColor.GRAY)
                      .build();
              audience.sendMessage(lastSeenMsg);
            });
  }

  @CommandAlias("alts|alternateaccounts")
  @Description("View a list of alternate accounts of a player")
  @CommandCompletion("@players")
  @Syntax("[target]")
  @CommandPermission(CommunityPermissions.LOOKUP)
  public void viewAlts(CommandAudience audience, @Optional String target) {
    if (target == null) {
      showOnlineAlts(audience, 1);
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

                        Component altNameList =
                            text()
                                .append(targetPlayer)
                                .append(text(" has "))
                                .append(
                                    text(alts.size(), NamedTextColor.YELLOW, TextDecoration.BOLD))
                                .append(text(" known alternate account"))
                                .append(text(alts.size() != 1 ? "s" : ""))
                                .append(text(": "))
                                .append(TextFormatter.list(altNames, NamedTextColor.GRAY))
                                .color(NamedTextColor.GRAY)
                                .build();
                        audience.sendMessage(altNameList);
                      });
            });
  }

  @CommandAlias("profile|user")
  @Description("View account info for a player")
  @Syntax("(name | uuid)")
  @CommandCompletion("@players")
  @CommandPermission(CommunityPermissions.LOOKUP)
  public void viewUserProfile(CommandAudience audience, String target) {
    users
        .getStoredProfile(target)
        .thenAcceptAsync(
            profile -> {
              if (profile == null) {
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
                  formatInfoField("Last Login", formatDateWithHover(profile.getLastLogin()));
              Component joinCount =
                  formatInfoField(
                      "Join Count", text(profile.getJoinCount(), NamedTextColor.YELLOW));

              Component lastServer =
                  formatInfoField(
                      "Last Server", text(profile.getServerName(), NamedTextColor.AQUA));

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
    Set<Player> accountedFor = Sets.newHashSet();

    for (Player player : Bukkit.getOnlinePlayers()) {
      Set<Player> alts = getAltAccounts(player);

      if (alts.isEmpty() || accountedFor.contains(player)) {
        continue;
      } else {
        altAccounts.add(formatAltAccountList(player, alts));
        accountedFor.add(player);
        accountedFor.addAll(alts);
      }
    }

    int perPage = Math.max(15, altAccounts.size());
    int pages = (altAccounts.size() + perPage - 1) / perPage;

    Component pageHeader =
        translatable(
            "command.pageHeader",
            NamedTextColor.GRAY,
            text(Integer.toString(page), NamedTextColor.DARK_AQUA),
            text(Integer.toString(pages), NamedTextColor.DARK_AQUA));

    Component headerText = translatable("moderation.alts.header", NamedTextColor.DARK_AQUA);

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

  private Component formatAltAccountList(Player target, Set<Player> alts) {
    Component names =
        Component.join(
            text(", ", NamedTextColor.GRAY),
            alts.stream()
                .map(pl -> PlayerComponent.player(pl, NameStyle.FANCY))
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

  private Set<Player> getAltAccounts(Player target) {
    Set<Player> sameIPs = Sets.newHashSet();
    String address = target.getAddress().getAddress().getHostAddress();

    for (Player other : Bukkit.getOnlinePlayers()) {
      if (other.getAddress().getAddress().getHostAddress().equals(address)
          && !other.equals(target)) {
        sameIPs.add(other);
      }
    }

    return sameIPs;
  }
}

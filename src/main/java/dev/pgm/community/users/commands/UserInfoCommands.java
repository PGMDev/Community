package dev.pgm.community.users.commands;

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
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.event.HoverEvent;
import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.PeriodFormats;
import tc.oc.pgm.util.text.TextFormatter;
import tc.oc.pgm.util.text.formatting.PaginatedComponentResults;
import tc.oc.pgm.util.text.types.PlayerComponent;

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

              Component lastSeenMsg =
                  TextComponent.builder()
                      .append(
                          PlayerComponent.of(
                              profile.getId(), profile.getUsername(), NameStyle.FANCY))
                      .append(
                          visible
                              ? " has been online since "
                              : " was last seen ") // TODO: translate
                      .append(
                          PeriodFormats.relativePastApproximate(profile.getLastLogin())
                              .color(online ? TextColor.GREEN : TextColor.DARK_GREEN))
                      .color(TextColor.GRAY)
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
                            PlayerComponent.of(
                                profile.getId(), profile.getUsername(), NameStyle.FANCY);
                        if (alts.isEmpty()) {
                          audience.sendWarning(
                              TranslatableComponent.of(
                                  "moderation.alts.noAlts", TextColor.GRAY, targetPlayer));
                          return;
                        }

                        Set<Component> altNames =
                            alts.stream()
                                .map(
                                    altId -> {
                                      Component name =
                                          PlayerComponent.of(
                                              altId,
                                              users.getStoredUsername(altId).join(),
                                              NameStyle.FANCY);

                                      return TextComponent.builder()
                                          .append(name)
                                          .clickEvent(
                                              ClickEvent.runCommand("/l " + altId.toString()))
                                          .hoverEvent(
                                              HoverEvent.showText(
                                                  TextComponent.builder()
                                                      .append(
                                                          "Click to view punishment history of ",
                                                          TextColor.GRAY)
                                                      .append(name)
                                                      .build()))
                                          .build();
                                    })
                                .collect(Collectors.toSet());

                        Component altNameList =
                            TextComponent.builder()
                                .append(targetPlayer)
                                .append(" has ")
                                .append(
                                    TextComponent.of(
                                        alts.size(), TextColor.YELLOW, TextDecoration.BOLD))
                                .append(" known alternate account")
                                .append(alts.size() != 1 ? "s" : "")
                                .append(": ")
                                .append(TextFormatter.list(altNames, TextColor.GRAY))
                                .color(TextColor.GRAY)
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
                  TextComponent.builder()
                      .append("Account Info", TextColor.RED)
                      .append(" - ", TextColor.GRAY)
                      .append(
                          PlayerComponent.of(
                              profile.getId(), profile.getUsername(), NameStyle.CONCISE))
                      .build();

              Component uuid =
                  formatInfoField(
                      "UUID", TextComponent.of(profile.getId().toString(), TextColor.YELLOW));
              Component firstLogin =
                  formatInfoField("First Login", formatDateWithHover(profile.getFirstLogin()));
              Component lastLogin =
                  formatInfoField("Last Login", formatDateWithHover(profile.getLastLogin()));
              Component joinCount =
                  formatInfoField(
                      "Join Count", TextComponent.of(profile.getJoinCount(), TextColor.YELLOW));

              Component lastServer =
                  formatInfoField(
                      "Last Server", TextComponent.of(profile.getServerName(), TextColor.AQUA));

              Component knownIPs = formatInfoField("Known IPs", TextComponent.empty());

              audience.sendMessage(
                  TextFormatter.horizontalLineHeading(
                      audience.getSender(), header, TextColor.DARK_GRAY));

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
                                              TextComponent.builder()
                                                  .append("      - ", TextColor.YELLOW)
                                                  .append(ip, TextColor.DARK_AQUA)
                                                  .build())
                                      .collect(Collectors.toList())));
                        });
              }
            });
  }

  private Component formatDateWithHover(Instant pastDate) {
    DateTimeFormatter timeFormat = DateTimeFormatter.ISO_INSTANT;
    return TextComponent.builder()
        .append(PeriodFormats.relativePastApproximate(pastDate))
        .hoverEvent(
            HoverEvent.showText(TextComponent.of(timeFormat.format(pastDate), TextColor.AQUA)))
        .build();
  }

  private Component formatListItems(Collection<Component> components) {
    return TextComponent.join(TextComponent.newline(), components);
  }

  private Component formatInfoField(String field, Component value) {
    return TextComponent.builder()
        .append(BroadcastUtils.BROADCAST_DIV)
        .append(TextComponent.of(field, TextColor.GOLD, TextDecoration.BOLD))
        .append(": ", TextColor.WHITE)
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
        TranslatableComponent.of(
            "command.pageHeader",
            TextColor.GRAY,
            TextComponent.of(Integer.toString(page), TextColor.DARK_AQUA),
            TextComponent.of(Integer.toString(pages), TextColor.DARK_AQUA));

    Component headerText = TranslatableComponent.of("moderation.alts.header", TextColor.DARK_AQUA);

    Component header =
        TextComponent.builder()
            .append(headerText)
            .append(" (", TextColor.GRAY)
            .append(Integer.toString(altAccounts.size()), TextColor.DARK_AQUA)
            .append(")", TextColor.GRAY)
            .append(" » ", TextColor.GOLD)
            .append(pageHeader)
            .build();

    Component formattedHeader =
        TextFormatter.horizontalLineHeading(audience.getSender(), header, TextColor.BLUE);

    new PaginatedComponentResults<Component>(formattedHeader, perPage) {
      @Override
      public Component format(Component data, int index) {
        return data;
      }

      @Override
      public Component formatEmpty() {
        // TODO: Translate
        return TextComponent.of("No alternate accounts found", TextColor.RED);
      }
    }.display(audience.getAudience(), altAccounts, page);
  }

  private Component formatAltAccountList(Player target, Set<Player> alts) {
    Component names =
        TextComponent.join(
            TextComponent.of(", ", TextColor.GRAY),
            alts.stream()
                .map(pl -> PlayerComponent.of(pl, NameStyle.FANCY))
                .collect(Collectors.toSet()));
    Component size = TextComponent.of(Integer.toString(alts.size()), TextColor.YELLOW);

    return TextComponent.builder()
        .append("[", TextColor.GOLD)
        .append(PlayerComponent.of(target, NameStyle.FANCY))
        .append("] ", TextColor.GOLD)
        .append("(", TextColor.GRAY)
        .append(size)
        .append("): ", TextColor.GRAY)
        .append(names)
        .build();
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

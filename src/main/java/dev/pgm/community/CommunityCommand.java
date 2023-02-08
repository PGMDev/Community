package dev.pgm.community;

import static net.kyori.adventure.text.Component.text;
import static tc.oc.pgm.util.player.PlayerComponent.player;

import com.google.common.collect.Sets;
import dev.pgm.community.users.feature.UsersFeature;
import dev.pgm.community.utils.CommandAudience;
import dev.pgm.community.utils.MessageUtils;
import dev.pgm.community.utils.PGMUtils;
import dev.pgm.community.utils.VisibilityUtils;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamMatchModule;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.TextException;
import tc.oc.pgm.util.text.TextFormatter;

public abstract class CommunityCommand {

  public static final String SELECTION = "<*, ?=1, team=Name, name1,name2...>";

  // Used to quickly format messages while in dev, move all final messages to TextComponents
  protected String format(String format, Object... args) {
    return MessageUtils.format(format, args);
  }

  protected CompletableFuture<Optional<UUID>> getTarget(String target, UsersFeature service) {
    boolean username = UsersFeature.USERNAME_REGEX.matcher(target).matches();
    if (!username) {
      try {
        return CompletableFuture.completedFuture(Optional.ofNullable(UUID.fromString(target)));
      } catch (IllegalArgumentException e) {
        throw TextException.exception(target + " is not a valid UUID.");
      }
    }
    return service.getStoredId(target);
  }

  public class PlayerSelection {

    private final Set<Player> players;
    private final Component selectionText;

    public PlayerSelection(Set<Player> players, Component selectionText) {
      this.players = players;
      this.selectionText = selectionText;
    }

    public Set<Player> getPlayers() {
      return players;
    }

    public Component getText() {
      List<Component> names =
          players.stream()
              .map(p -> player(p, NameStyle.FANCY))
              .limit(Math.min(players.size(), 10))
              .collect(Collectors.toList());

      Component hover = TextFormatter.list(names, NamedTextColor.GRAY);
      if (getPlayers().size() > names.size()) {
        int leftOver = getPlayers().size() - names.size();
        hover
            .append(text(" plus "))
            .append(text(leftOver, NamedTextColor.YELLOW))
            .append(text(" other player" + (leftOver != 1 ? "s" : "")))
            .color(NamedTextColor.GRAY);
      }
      return selectionText.hoverEvent(HoverEvent.showText(hover));
    }

    public void sendNoPlayerComponent(CommandAudience audience) {
      audience.sendWarning(text().append(text("No matching players found")).build());
    }
  }

  // * = all
  // ? = single random
  // ?=# = # of random
  // team=name = only 1 PGM team
  protected PlayerSelection getPlayers(CommandAudience viewer, String input) {
    boolean isAll =
        input.equalsIgnoreCase("*") && viewer.hasPermission(CommunityPermissions.ALL_SELECTOR);
    boolean isRandom =
        input.startsWith("?") && viewer.hasPermission(CommunityPermissions.RANDOM_SELECTOR);
    boolean isTeam =
        input.startsWith("team=")
            && PGMUtils.isPGMEnabled()
            && viewer.hasPermission(CommunityPermissions.TEAM_SELECTOR);

    String[] parts = input.split("=");

    List<Player> allOnline = Bukkit.getOnlinePlayers().stream().collect(Collectors.toList());

    Set<Player> targets = Sets.newHashSet();
    Component text;
    if (isAll) {
      targets.addAll(allOnline);
      text =
          text()
              .append(text("everyone "))
              .append(text("("))
              .append(text(targets.size(), NamedTextColor.GREEN))
              .append(text(")"))
              .color(NamedTextColor.GRAY)
              .build();
    } else if (isRandom) {
      int randomCount = parts.length == 2 ? parseInputInt(input, 1) : 1;
      for (int i = 0; i < randomCount; i++) {
        targets.add(allOnline.get(Community.get().getRandom().nextInt(allOnline.size())));
      }
      String rdTxt = " randomly chosen player" + (targets.size() != 1 ? "s" : "");
      text =
          text()
              .append(text(targets.size(), NamedTextColor.GREEN))
              .append(text(rdTxt, NamedTextColor.GRAY))
              .build();
    } else if (isTeam) {
      Match match = PGMUtils.getMatch();
      if (match.getModule(TeamMatchModule.class) != null) {
        TeamMatchModule teams = match.getModule(TeamMatchModule.class);
        String teamName = parts[1];
        if (teamName == null || teamName.isEmpty()) {
          throw TextException.exception("Please provide a team name");
        }

        // Allow Observers to be selected
        if (teamName.toLowerCase().startsWith("obs")) {
          text = text("Observers", NamedTextColor.AQUA);
          targets.addAll(
              match.getObservers().stream()
                  .map(MatchPlayer::getBukkit)
                  .collect(Collectors.toList()));
        } else {
          Team team = teams.bestFuzzyMatch(teamName);
          if (team == null) {
            throw TextException.exception(teamName + " is not a valid team name");
          }
          targets.addAll(
              team.getPlayers().stream().map(MatchPlayer::getBukkit).collect(Collectors.toList()));
          text =
              text()
                  .append(text(team.getNameLegacy(), TextFormatter.convert(team.getColor())))
                  .build();
        }
      } else {
        throw TextException.exception("There are no teams in this match to select");
      }
    } else {
      String[] names = input.split(",");
      for (String name : names) {
        Player player = Bukkit.getPlayer(name);
        if (player != null) {
          targets.add(player);
        }
      }
      text =
          text()
              .append(text(targets.size(), NamedTextColor.GREEN))
              .append(text(" player" + (targets.size() != 1 ? "s" : "")))
              .color(NamedTextColor.GRAY)
              .build();

      if (targets.size() > 1 && !viewer.hasPermission(CommunityPermissions.SELECTOR)) {
        // If no permission for multiple, get a random single entry
        Player random = targets.stream().findAny().get();
        targets.clear();
        if (random != null) {
          targets.add(random);
        }
      }
    }

    return new PlayerSelection(targets, text);
  }

  private int parseInputInt(String input, int def) {
    int value = def;
    try {
      value = Integer.parseInt(input);
    } catch (NumberFormatException e) {
      value = def;
    }
    return value;
  }

  private boolean isNicked(CommandAudience viewer, Player player) {
    if (viewer.hasPermission(CommunityPermissions.STAFF)) return false;
    return Community.get().getFeatures().getNick().isNicked(player.getUniqueId());
  }

  @Nullable
  protected Player getSinglePlayer(CommandAudience viewer, String target, boolean allowNicks) {
    Player player = Bukkit.getPlayer(target);
    Player nicked = Community.get().getFeatures().getNick().getPlayerFromNick(target);

    if (player == null && nicked != null && allowNicks) {
      player = nicked;
    }

    if (player == null
        || (player != null && !canViewVanished(viewer, player))
        || (player != null && nicked == null && isNicked(viewer, player))) {
      viewer.sendWarning(formatNotFoundComponent(target));
      return null;
    }

    return player;
  }

  protected UUID getOnlineTarget(String target, UsersFeature service) {
    boolean username = UsersFeature.USERNAME_REGEX.matcher(target).matches();
    UUID id = null;
    if (!username) {
      try {
        id = UUID.fromString(target);
      } catch (IllegalArgumentException e) {
        throw TextException.exception(target + " is not a valid UUID.");
      }
    }

    if (id == null) {
      // TODO: Maybe use getStoredID and listen, that way we can account for EVERYONE. But not a
      // priority now
      Optional<UUID> cachedId =
          service.getId(
              target); // If user is online or was online recently, we will have their UUID.
      if (!cachedId.isPresent()) {
        throw TextException.exception(formatNotFoundMsg(target));
      } else {
        id = cachedId.get();
      }
    }

    return id;
  }

  protected boolean isDisguised(CommandAudience audience) {
    return !audience.isPlayer() || isDisguised(audience.getPlayer());
  }

  protected boolean isDisguised(Player player) {
    return VisibilityUtils.isDisguised(player);
  }

  private boolean isVanished(@Nullable Player player) {
    return player != null && player.hasMetadata("isVanished");
  }

  public boolean canViewVanished(CommandAudience viewer, Player player) {
    boolean vanished = isVanished(player);
    if (vanished
        && viewer.isPlayer()
        && !viewer.getPlayer().hasPermission(CommunityPermissions.VIEW_VANISHED)) {
      return false;
    }
    return true;
  }

  protected String formatNotFoundMsg(String target) {
    return ChatColor.AQUA + target + ChatColor.RED + " could not be found.";
  }

  protected Component formatNotFoundComponent(String target) {
    return text()
        .append(text(target, NamedTextColor.AQUA))
        .append(text(" could not be found.", NamedTextColor.RED))
        .build();
  }

  protected Component button(String text, NamedTextColor color, String command, String hover) {
    return button(
        text(text, color, TextDecoration.BOLD), command, text(hover, NamedTextColor.GRAY));
  }

  protected Component button(Component component, String command, Component hover) {
    return text()
        .append(text("["))
        .append(component)
        .append(text("]"))
        .color(NamedTextColor.GRAY)
        .clickEvent(ClickEvent.runCommand(command))
        .hoverEvent(HoverEvent.showText(hover))
        .build();
  }
}

package dev.pgm.community.nick.commands;

import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;

import dev.pgm.community.Community;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.commands.target.TargetPlayer;
import dev.pgm.community.nick.feature.NickFeature;
import dev.pgm.community.users.feature.UsersFeature;
import dev.pgm.community.utils.CommandAudience;
import dev.pgm.community.utils.NameUtils;
import dev.pgm.community.utils.PaginatedComponentResults;
import dev.pgm.community.utils.WebUtils;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
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
import tc.oc.pgm.lib.cloud.commandframework.annotations.Flag;
import tc.oc.pgm.lib.cloud.commandframework.annotations.ProxiedBy;
import tc.oc.pgm.util.bukkit.BukkitUtils;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.player.PlayerComponent;
import tc.oc.pgm.util.text.TextException;
import tc.oc.pgm.util.text.TextFormatter;

@CommandMethod("nick")
public class NickCommands extends CommunityCommand {

  private final NickFeature nicks;
  private final UsersFeature users;

  public NickCommands() {
    this.nicks = Community.get().getFeatures().getNick();
    this.users = Community.get().getFeatures().getUsers();
  }

  @CommandMethod("")
  @CommandDescription("Check your current nickname status")
  public void checkNickStatus(
      CommandAudience viewer, @Flag(value = "target", aliases = "t") String target) {
    // Check nick status of other players
    if (viewer.hasPermission(CommunityPermissions.NICKNAME_OTHER) && target != null) {
      getTarget(target, users)
          .thenAcceptAsync(
              uuid -> {
                if (uuid.isPresent()) {
                  users
                      .renderUsername(uuid, NameStyle.FANCY)
                      .thenAcceptAsync(
                          name -> {
                            sendNickStatus(viewer, viewer.getPlayer(), uuid.get(), name);
                            return;
                          });
                } else {
                  viewer.sendWarning(formatNotFoundComponent(target));
                }
              });
    } else {
      if (!viewer.isPlayer()) return;

      // Own status
      sendNickStatus(
          viewer, viewer.getPlayer(), viewer.getPlayer().getUniqueId(), viewer.getStyledName());
    }
  }

  @CommandMethod("random [page]")
  @CommandDescription("Set a random nickname")
  @CommandPermission(CommunityPermissions.NICKNAME)
  public void setRandomNick(
      CommandAudience viewer,
      Player sender,
      @Argument(value = "page", defaultValue = "1") int page) {
    nicks
        .getNickSelection(sender.getUniqueId())
        .thenAcceptAsync(
            names -> {
              List<String> selection = names.getNames();

              int resultsPerPage = 8;
              int pages = (selection.size() + resultsPerPage - 1) / resultsPerPage;

              Component formattedTitle =
                  TextFormatter.horizontalLineHeading(
                      viewer.getSender(), text("Select a nickname"), NamedTextColor.DARK_AQUA, 250);

              new PaginatedComponentResults<String>(formattedTitle, resultsPerPage) {
                @Override
                public Component format(String nick, int index) {
                  return text()
                      .append(text(" - ", NamedTextColor.GOLD))
                      .append(text(nick, NamedTextColor.YELLOW))
                      .hoverEvent(
                          HoverEvent.showText(
                              text("Click to set nick to ", NamedTextColor.GRAY)
                                  .append(text(nick, NamedTextColor.YELLOW))))
                      .clickEvent(ClickEvent.runCommand("/nick confirm " + nick))
                      .color(NamedTextColor.GRAY)
                      .build();
                }

                @Override
                public Component formatEmpty() {
                  return text("Issue loading names, please try again!", NamedTextColor.RED);
                }
              }.display(viewer.getAudience(), selection, page);

              // Add page button when more than 1 page
              if (pages > 1) {
                TextComponent.Builder buttons = text();

                if (page > 1) {
                  buttons.append(
                      text()
                          .append(text("Click for more names", NamedTextColor.BLUE))
                          .hoverEvent(
                              HoverEvent.showText(
                                  text("Click to refresh nick selection", NamedTextColor.GRAY)))
                          .clickEvent(ClickEvent.runCommand("/nick random " + (page - 1))));
                }

                if (page > 1 && page < pages) {
                  buttons.append(text(" | ", NamedTextColor.DARK_GRAY));
                }

                if (page < pages) {
                  buttons.append(
                      text()
                          .append(text("Click for more names", NamedTextColor.BLUE))
                          .hoverEvent(
                              HoverEvent.showText(
                                  text("Click to refresh nick selection", NamedTextColor.GRAY)))
                          .clickEvent(ClickEvent.runCommand("/nick random " + (page + 1))));
                }
                viewer.sendMessage(
                    TextFormatter.horizontalLineHeading(
                        viewer.getSender(), buttons.build(), NamedTextColor.DARK_AQUA, 250));
              }
            });
  }

  @CommandMethod("confirm <name>")
  @CommandDescription("Confirm random nickname choice")
  public void selectNick(CommandAudience viewer, Player sender, @Argument("name") String name) {
    nicks
        .getNickSelection(sender.getUniqueId())
        .thenAcceptAsync(
            selection -> {
              if (!selection.isValid(name)) {
                viewer.sendWarning(
                    text()
                        .append(text(name, NamedTextColor.YELLOW))
                        .append(text(" is not a valid nick"))
                        .build());
                viewer.sendWarning(text("Please select one below:", NamedTextColor.RED));
                setRandomNick(viewer, sender, 1);
                return;
              }
              setOwnNick(viewer, sender, name);
            });
  }

  @CommandMethod("skin <name>")
  @CommandDescription("Set skin for current nick session")
  @CommandPermission(CommunityPermissions.NICKNAME_SET)
  public void setOwnSkin(CommandAudience viewer, Player sender, @Argument("name") String name) {

    if (name.equalsIgnoreCase("reset") || name.equalsIgnoreCase("clear")) {
      nicks.getSkinManager().setSkin(viewer.getPlayer(), null);
      viewer.sendWarning(text("You have reset your skin"));
      return;
    }

    WebUtils.getSkin(name)
        .thenAcceptAsync(
            skin -> {
              if (skin == null) {
                viewer.sendWarning(
                    text()
                        .append(text("No skin was found for "))
                        .append(text(name, NamedTextColor.AQUA))
                        .build());
                return;
              }
              // Run sync
              Bukkit.getScheduler()
                  .runTask(
                      Community.get(),
                      () -> nicks.getSkinManager().setSkin(viewer.getPlayer(), skin));
              viewer.sendMessage(
                  text()
                      .append(text("You have set your custom skin to "))
                      .append(text(name, NamedTextColor.AQUA))
                      .color(NamedTextColor.GRAY)
                      .build());
            });
  }

  // /nick set [name]
  @CommandMethod("set <nick>")
  @CommandDescription("Set your nickname")
  @CommandPermission(CommunityPermissions.NICKNAME_SET)
  public void setOwnNick(CommandAudience viewer, Player sender, @Argument("nick") String nick) {
    if (nick == null) {
      checkNickStatus(viewer, null);
      return;
    }

    // Ensure nickname conforms to minecraft standards
    validateNick(nick);

    nicks
        .setNick(viewer.getPlayer().getUniqueId(), nick)
        .thenAcceptAsync(
            success -> {
              if (success) {
                Component header =
                    TextFormatter.horizontalLine(NamedTextColor.GRAY, TextFormatter.MAX_CHAT_WIDTH);

                Component msg =
                    text()
                        .append(text("You have set your nickname to "))
                        .append(text(nick, NamedTextColor.AQUA, TextDecoration.BOLD))
                        .append(newline())
                        .append(
                            text(
                                "Your nickname will be active upon next login",
                                NamedTextColor.GREEN))
                        .color(NamedTextColor.GRAY)
                        .build();

                viewer.sendMessage(
                    text()
                        .append(header)
                        .append(newline())
                        .append(msg)
                        .append(newline())
                        .append(header)
                        .build());
              } else {
                viewer.sendWarning(
                    text()
                        .append(text("Could not set nickname to "))
                        .append(text(nick, NamedTextColor.AQUA))
                        .build());
              }
            });
  }

  @CommandMethod("setother <target> <nick>")
  @CommandDescription("Set the nickname of another player")
  @CommandPermission(CommunityPermissions.NICKNAME_OTHER)
  public void setOtherNick(
      CommandAudience viewer,
      @Argument("target") TargetPlayer target,
      @Argument("nick") String nick) {
    validateNick(nick);
    getTarget(target.getIdentifier(), users)
        .thenAcceptAsync(
            uuid -> {
              if (uuid.isPresent()) {
                nicks
                    .setNick(uuid.get(), nick)
                    .thenAcceptAsync(
                        success -> {
                          if (success) {
                            users
                                .renderUsername(uuid, NameStyle.FANCY)
                                .thenAcceptAsync(
                                    name -> {
                                      viewer.sendMessage(
                                          text()
                                              .append(text("Nickname for "))
                                              .append(name)
                                              .append(text(" set to "))
                                              .append(
                                                  text(
                                                      nick,
                                                      NamedTextColor.AQUA,
                                                      TextDecoration.BOLD))
                                              .color(NamedTextColor.GRAY)
                                              .build());
                                    });
                          } else {
                            viewer.sendWarning(
                                text()
                                    .append(text("Could not set nickname for "))
                                    .append(text(target.getIdentifier(), NamedTextColor.AQUA))
                                    .build());
                          }
                        });

              } else {
                viewer.sendWarning(formatNotFoundComponent(target.getIdentifier()));
              }
            });
  }

  @CommandMethod("clear [target]")
  @CommandDescription("Remove nickname from yourself or another player")
  public void clearNick(CommandAudience viewer, @Argument("target") TargetPlayer target) {
    // Clear other user names
    if (viewer.hasPermission(CommunityPermissions.NICKNAME_CLEAR) && target != null) {
      getTarget(target.getIdentifier(), users)
          .thenAcceptAsync(
              uuid -> {
                if (uuid.isPresent()) {
                  users
                      .renderUsername(uuid, NameStyle.FANCY)
                      .thenAcceptAsync(
                          name -> {
                            nicks
                                .clearNick(uuid.get())
                                .thenAcceptAsync(
                                    success -> {
                                      Component setName =
                                          text()
                                              .append(text("You have reset the nickname of "))
                                              .append(name)
                                              .color(NamedTextColor.GREEN)
                                              .build();
                                      Component noName =
                                          text()
                                              .append(name)
                                              .append(text(" does not have a nickname set"))
                                              .color(NamedTextColor.RED)
                                              .build();
                                      viewer.sendWarning(success ? setName : noName);
                                    });
                          });
                } else {
                  viewer.sendWarning(formatNotFoundComponent(target.getIdentifier()));
                }
              });
      return;
    }

    if (!viewer.isPlayer()) return;

    // Reset own nickname
    nicks
        .clearNick(viewer.getPlayer().getUniqueId())
        .thenAcceptAsync(
            success -> {
              viewer.sendWarning(
                  text(
                      success ? "You have reset your nickname" : "You do not have a nickname set",
                      success ? NamedTextColor.GRAY : NamedTextColor.RED));
            });
  }

  @CommandMethod("toggle")
  @CommandDescription("Toggle your nickname status")
  public void enableNick(CommandAudience viewer, Player sender) {
    nicks
        .toggleNick(sender.getUniqueId())
        .thenAcceptAsync(
            result ->
                viewer.sendWarning(
                    text()
                        .append(text("You have "))
                        .append(
                            result
                                ? text("enabled", NamedTextColor.GREEN)
                                : text("disabled", NamedTextColor.RED))
                        .append(text(" your nickname."))
                        .color(NamedTextColor.GRAY)
                        .build()));
  }

  @ProxiedBy("nicks")
  @CommandMethod("list")
  @CommandDescription("View a list of online nicked players")
  @CommandPermission(CommunityPermissions.STAFF)
  public void viewNicks(CommandAudience viewer) {
    List<Component> nickedNames =
        Bukkit.getOnlinePlayers().stream()
            .filter(player -> nicks.isNicked(player.getUniqueId()))
            .map(player -> PlayerComponent.player(player, NameStyle.FANCY))
            .collect(Collectors.toList());

    if (nickedNames.isEmpty()) {
      viewer.sendWarning(text("No online players are nicked!"));
      return;
    }

    Component count =
        text()
            .append(text("Nicked", NamedTextColor.DARK_AQUA))
            .append(text(": "))
            .append(text(nickedNames.size()))
            .build();
    Component nameList = TextFormatter.list(nickedNames, NamedTextColor.GRAY);

    viewer.sendMessage(count);
    viewer.sendMessage(nameList);
  }

  private void sendNickStatus(
      CommandAudience viewer, @Nullable Player sender, UUID targetId, Component targetName) {
    final boolean self = sender != null && sender.getUniqueId().equals(targetId);
    nicks
        .getNick(targetId)
        .thenAcceptAsync(
            nick -> {
              if (nick == null || nick.getName().isEmpty()) {
                if (self) {
                  // No nickname set, then random one will be assigned
                  setRandomNick(viewer, sender, 1);
                } else {
                  // Other target has no nickname
                  viewer.sendWarning(
                      text()
                          .append(targetName)
                          .append(text(" does not have a nickname set"))
                          .build());
                }
                return;
              }

              Component newName =
                  createTextButton(
                      "New",
                      "/nick random",
                      "&7Click to recieve a new nickname",
                      NamedTextColor.AQUA);

              // Only display toggle to users who can set a custom nickname
              Component toggle =
                  createTextButton(
                      "Toggle",
                      "/nick toggle",
                      "&7Click to "
                          + (nick.isEnabled() ? "&cdisable" : "&aenable")
                          + "&7 your nickname",
                      nick.isEnabled() ? NamedTextColor.RED : NamedTextColor.GREEN);

              Component clear =
                  createTextButton(
                      "Clear",
                      "/nick clear",
                      "Click to clear your nickname",
                      NamedTextColor.DARK_RED);

              TextComponent.Builder statusMsg =
                  text()
                      .append(text("Nickname: ("))
                      .append(text(nick.getName(), NamedTextColor.AQUA))
                      .append(text(") Status: "))
                      .append(
                          text(
                              nick.isEnabled() ? "Enabled" : "Disabled",
                              nick.isEnabled() ? NamedTextColor.GREEN : NamedTextColor.RED));

              if (self) {
                statusMsg
                    .append(space())
                    .append(newName)
                    .append(space())
                    .append(toggle)
                    .append(space())
                    .append(clear);
              }
              if (!self)
                viewer.sendMessage(
                    text()
                        .append(text("Nick Status for ", NamedTextColor.GRAY))
                        .append(targetName)
                        .build());
              viewer.sendMessage(statusMsg.color(NamedTextColor.GRAY).build());

              // Alert user about nickname updates
              if (self) {
                if (!nicks.isNicked(viewer.getPlayer().getUniqueId()) && nick.isEnabled()) {
                  viewer.sendWarning(
                      text(
                          "Your nickname will be active upon your next login",
                          NamedTextColor.GREEN));
                } else if (nicks.isNicked(viewer.getPlayer().getUniqueId()) && !nick.isEnabled()) {
                  viewer.sendMessage(
                      text("Your nickname will be removed once you logout", NamedTextColor.RED));
                }
              }
            });
  }

  private Component createTextButton(
      String text, String command, String hover, NamedTextColor color) {
    return text()
        .append(text("[", NamedTextColor.GRAY))
        .append(text(text, color, TextDecoration.BOLD))
        .append(text("]", NamedTextColor.GRAY))
        .clickEvent(ClickEvent.runCommand(command))
        .hoverEvent(HoverEvent.showText(text(BukkitUtils.colorize(hover), NamedTextColor.GRAY)))
        .build();
  }

  private void validateNick(String name) throws TextException {
    if (!NameUtils.isMinecraftName(name)) {
      throw TextException.exception(name + " is not a valid minecraft username");
    }
  }
}

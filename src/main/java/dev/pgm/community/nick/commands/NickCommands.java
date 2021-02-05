package dev.pgm.community.nick.commands;

import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;

import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Flags;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.nick.feature.NickFeature;
import dev.pgm.community.users.feature.UsersFeature;
import dev.pgm.community.utils.CommandAudience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import tc.oc.pgm.util.bukkit.BukkitUtils;
import tc.oc.pgm.util.text.TextFormatter;

@CommandAlias("nick")
@CommandPermission(CommunityPermissions.NICKNAME)
public class NickCommands extends CommunityCommand {

  @Dependency private NickFeature nicks;
  @Dependency private UsersFeature users;

  private String getRandomName() {
    // TODO: Random username gen still required
    return "RANDOM";
  }

  @Subcommand("random")
  @Description("Set a random nickname")
  public void setRandomNick(CommandAudience viewer, Player player) {
    String randomName = getRandomName();
    setOwnNick(viewer, player, randomName);
  }

  // /nick set [name]
  @Subcommand("set")
  @Description("Set your nickname")
  @Syntax("[name]")
  @CommandPermission(CommunityPermissions.NICKNAME_SET)
  public void setOwnNick(CommandAudience viewer, Player player, @Optional String nick) {
    if (nick == null) {
      checkNickStatus(viewer, player, null);
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

  // TODO
  @Subcommand("setother|other")
  @Description("Set the nickname of another player")
  @Syntax("[target] [nick]")
  public void setOtherNick(
      CommandAudience viewer, @Flags("other") Player target, @Optional String nick) {}

  @Subcommand("status")
  @Description("Check your current nickname status")
  @Syntax("[target]")
  @Default
  public void checkNickStatus(CommandAudience viewer, Player player, @Optional String target) {
    nicks
        .getNick(viewer.getPlayer().getUniqueId())
        .thenAcceptAsync(
            nick -> {
              if (nick == null) {
                viewer.sendWarning(
                    text()
                        .append(
                            text("You do not have a nickname set! ", NamedTextColor.RED)
                                .append(
                                    createTextButton(
                                        "Get Random Name",
                                        "/nick random",
                                        "Click to recieve a new nickname",
                                        NamedTextColor.GREEN)))
                        .build());
                return;
              }

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

              viewer.sendMessage(
                  text()
                      .append(text("Nickname: ("))
                      .append(text(nick.getNickName(), NamedTextColor.AQUA))
                      .append(text(") Status: "))
                      .append(
                          text(
                              nick.isEnabled() ? "Enabled" : "Disabled",
                              nick.isEnabled() ? NamedTextColor.GREEN : NamedTextColor.RED))
                      .append(space())
                      .append(toggle)
                      .append(space())
                      .append(clear)
                      .color(NamedTextColor.GRAY)
                      .build());

              // Alert user about nickname updates
              if (!nicks.isNicked(viewer.getPlayer().getUniqueId()) && nick.isEnabled()) {
                viewer.sendWarning(
                    text(
                        "Your nickname will be active upon your next login", NamedTextColor.GREEN));
              } else if (nicks.isNicked(viewer.getPlayer().getUniqueId()) && !nick.isEnabled()) {
                viewer.sendMessage(
                    text("Your nickname will be removed once you logout", NamedTextColor.RED));
              }
            });
  }

  @Subcommand("clear|reset")
  @Description("Remove your nickname")
  public void clearNick(CommandAudience viewer, Player player) {
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

  @Subcommand("toggle")
  @Description("Toggle your nickname status")
  public void enableNick(CommandAudience viewer, Player player) {
    nicks
        .toggleNick(viewer.getPlayer().getUniqueId())
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

  @Subcommand("check")
  @Description("Check if the provided name is available")
  public void checkNick(CommandAudience viewer, String nick) {
    validateNick(nick);
    nicks
        .isNameAvailable(nick)
        .thenAcceptAsync(
            available -> {
              viewer.sendWarning(
                  text()
                      .append(
                          text(
                              nick,
                              available ? NamedTextColor.GREEN : NamedTextColor.RED,
                              TextDecoration.BOLD))
                      .append(text(" is "))
                      .append(text(available ? "available for use" : "unavailable at this time."))
                      .color(NamedTextColor.GRAY)
                      .build());
            });
  }

  //  // TODO: bring this back later, allows for nickname history to be viewed
  //  // COLOR THE >> & the icon as the same color to indicate status
  //  private Component formatPastNicks(Nick nick) {
  //    return TextComponent.builder()
  //        .append(
  //            (nick.isValid()
  //                ? MessageUtils.WARNING.append(TextComponent.space())
  //                : TextComponent.of("- ", TextColor.YELLOW)))
  //        .append(users.getStoredUsername(nick.getPlayerId()).join(), TextColor.DARK_AQUA)
  //        .append(" ")
  //        .append(BroadcastUtils.RIGHT_DIV.color(nick.isValid() ? TextColor.GREEN :
  // TextColor.YELLOW))
  //        .append(" ")
  //        .append(PeriodFormats.relativePastApproximate(nick.getDateSet()).color(TextColor.BLUE))
  //        .hoverEvent(
  //            HoverEvent.showText(
  //                TextComponent.of(
  //                    nick.isValid() ? "Unavailable at this time" : "Available for use",
  //                    TextColor.GRAY)))
  //        .build();
  //  }

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

  private void validateNick(String name) throws InvalidCommandArgument {
    if (!UsersFeature.USERNAME_REGEX.matcher(name).matches()) {
      throw new InvalidCommandArgument(name + " is not a valid minecraft username", false);
    }
  }
}
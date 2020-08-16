package dev.pgm.community.users.commands;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Syntax;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.users.feature.UsersFeature;
import dev.pgm.community.utils.CommandAudience;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.PeriodFormats;
import tc.oc.pgm.util.text.types.PlayerComponent;

public class ProfileCommands extends CommunityCommand {

  @Dependency private UsersFeature users;

  // TODO: take into account vanished, find a way to hide em
  @CommandAlias("seen|lastseen")
  @Description("View when a player was last online")
  @Syntax("[player]")
  @CommandCompletion("@players")
  public void seenPlayer(CommandAudience audience, String target) {
    users
        .getStoredProfile(target)
        .thenAcceptAsync(
            profile -> {
              if (profile != null) {
                boolean online = Bukkit.getPlayer(profile.getId()) != null;
                Component lastSeenMsg =
                    TextComponent.builder()
                        .append(
                            PlayerComponent.of(
                                profile.getId(), profile.getUsername(), NameStyle.FANCY))
                        .append(
                            online
                                ? " has been online since "
                                : " was last seen ") // TODO: translate
                        .append(
                            PeriodFormats.relativePastApproximate(profile.getLastLogin())
                                .color(online ? TextColor.GREEN : TextColor.YELLOW))
                        .color(TextColor.GRAY)
                        .build();
                audience.sendMessage(lastSeenMsg);
              }
            });
  }

  // TODO: This is a debug command
  @CommandAlias("profile")
  public void viewProfile(CommandSender sender) {
    users
        .getStoredProfile(((Player) sender).getUniqueId())
        .thenAcceptAsync(
            profile -> {
              sender.sendMessage("Profile: " + profile.getUsername());
              sender.sendMessage("UUID: " + profile.getId());
              sender.sendMessage("First: " + profile.getFirstLogin());
              sender.sendMessage("Last: " + profile.getLastLogin());
              sender.sendMessage("Joins: " + profile.getJoinCount());
            });
  }
}

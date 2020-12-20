package dev.pgm.community.moderation.commands;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Syntax;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.moderation.feature.ModerationFeature;
import dev.pgm.community.moderation.punishments.PunishmentType;
import dev.pgm.community.users.feature.UsersFeature;
import dev.pgm.community.utils.BroadcastUtils;
import dev.pgm.community.utils.CommandAudience;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.PlayerComponent;

public class MuteCommand extends CommunityCommand {

  @Dependency private ModerationFeature moderation;
  @Dependency private UsersFeature usernames;

  @CommandAlias("mute|m")
  @Description("Prevent player from speaking in the chat")
  @Syntax("[player] [duration] [reason]")
  @CommandCompletion("@players 30m|1h|6h *")
  @CommandPermission(CommunityPermissions.MUTE)
  public void mutePlayer(CommandAudience audience, String target, Duration length, String reason) {
    getTarget(target, usernames)
        .thenAccept(
            id -> {
              if (id.isPresent()) {
                moderation.punish(
                    PunishmentType.MUTE,
                    id.get(),
                    audience,
                    reason,
                    length,
                    true,
                    isVanished(audience));
              } else {
                audience.sendWarning(formatNotFoundComponent(target));
              }
            });
  }

  @CommandAlias("unmute|um")
  @Description("Unmute a player")
  @Syntax("[player]")
  @CommandCompletion("@mutes")
  @CommandPermission(CommunityPermissions.MUTE)
  public void unMutePlayer(CommandAudience audience, String target) {
    getTarget(target, usernames)
        .thenAccept(
            id -> {
              if (id.isPresent()) {

                moderation
                    .isMuted(id.get())
                    .thenAcceptAsync(
                        isMuted -> {
                          if (isMuted.isPresent()) {
                            moderation
                                .unmute(id.get(), audience.getId())
                                .thenAcceptAsync(
                                    pardon -> {
                                      if (!pardon) {
                                        audience.sendWarning(
                                            usernames
                                                .renderUsername(id)
                                                .join()
                                                .append(text(" could not be ", NamedTextColor.GRAY))
                                                .append(text("unmuted"))
                                                .color(NamedTextColor.RED));
                                      } else {
                                        BroadcastUtils.sendAdminChatMessage(
                                            usernames
                                                .renderUsername(id)
                                                .join()
                                                .append(
                                                    text(" was unmuted by ", NamedTextColor.GRAY))
                                                .append(audience.getStyledName()),
                                            null);

                                        Player online = Bukkit.getPlayer(id.get());
                                        if (online != null) {
                                          Audience.get(online)
                                              .sendWarning(
                                                  translatable(
                                                      "moderation.unmute.target",
                                                      NamedTextColor.GREEN));
                                        }
                                      }
                                    });
                          } else {
                            audience.sendWarning(
                                usernames
                                    .renderUsername(id)
                                    .join()
                                    .append(text(" is not muted", NamedTextColor.GRAY)));
                          }
                        });
              }
            });
  }

  @CommandAlias("mutes")
  @Description("List all online players who are muted")
  @CommandPermission(CommunityPermissions.MUTE)
  public void listOnlineMuted(CommandAudience audience) {
    Set<Player> mutedPlayers = moderation.getOnlineMutes();

    List<Component> onlineMutes =
        mutedPlayers.stream()
            .map(player -> PlayerComponent.player(player, NameStyle.FANCY))
            .collect(Collectors.toList());

    if (onlineMutes.isEmpty()) {
      audience.sendWarning(translatable("moderation.mute.none"));
      return;
    }

    Component names = Component.join(text(", ", NamedTextColor.GRAY), onlineMutes);
    Component message =
        translatable("moderation.mute.list", NamedTextColor.GOLD)
            .append(text("(", NamedTextColor.GRAY))
            .append(text(Integer.toString(onlineMutes.size()), NamedTextColor.YELLOW))
            .append(text("): ", NamedTextColor.GRAY))
            .append(names);

    audience.sendMessage(message);
  }
}

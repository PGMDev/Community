package dev.pgm.community.moderation.commands;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Syntax;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.moderation.feature.ModerationFeature;
import dev.pgm.community.moderation.punishments.PunishmentType;
import dev.pgm.community.utils.BroadcastUtils;
import dev.pgm.community.utils.CommandAudience;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.entity.Player;
import tc.oc.pgm.util.chat.Audience;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.types.PlayerComponent;

public class MuteCommand extends CommunityCommand {

  @Dependency private ModerationFeature moderation;

  @CommandAlias("mute|m")
  @Description("Prevent player from speaking in the chat")
  @Syntax("[player] [duration] [reason]")
  @CommandCompletion("@players 30m|1h|6h *")
  @CommandPermission(CommunityPermissions.MUTE)
  public void mutePlayer(
      CommandAudience audience, OnlinePlayer target, Duration length, String reason) {
    moderation.punish(
        PunishmentType.MUTE,
        target.getPlayer().getUniqueId(),
        audience,
        reason,
        length,
        true,
        isVanished(audience));
  }

  @CommandAlias("unmute|um")
  @Description("Unmute a player")
  @Syntax("[player]")
  @CommandCompletion("@mutes")
  @CommandPermission(CommunityPermissions.MUTE)
  public void unMutePlayer(CommandAudience audience, OnlinePlayer target) {
    moderation
        .isMuted(target.getPlayer().getUniqueId())
        .thenAcceptAsync(
            isMuted -> {
              if (isMuted.isPresent()) {
                moderation
                    .unmute(target.getPlayer().getUniqueId(), audience.getId())
                    .thenAcceptAsync(
                        pardon -> {
                          if (!pardon) {
                            audience.sendWarning(
                                TextComponent.builder()
                                    .append(PlayerComponent.of(target.getPlayer(), NameStyle.FANCY))
                                    .append(" could not be ", TextColor.GRAY)
                                    .append("unmuted")
                                    .color(TextColor.RED)
                                    .build());
                          } else {
                            BroadcastUtils.sendAdminChatMessage(
                                TextComponent.builder()
                                    .append(PlayerComponent.of(target.getPlayer(), NameStyle.FANCY))
                                    .append(" was unmuted by ", TextColor.GRAY)
                                    .append(audience.getStyledName())
                                    .build(),
                                null);

                            Audience.get(target.getPlayer())
                                .sendWarning(
                                    TranslatableComponent.of(
                                        "moderation.unmute.target", TextColor.GREEN));
                          }
                        });
              } else {
                audience.sendWarning(
                    TextComponent.builder()
                        .append(PlayerComponent.of(target.getPlayer(), NameStyle.FANCY))
                        .append(" is not muted", TextColor.GRAY)
                        .build());
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
            .map(player -> PlayerComponent.of(player, NameStyle.FANCY))
            .collect(Collectors.toList());

    if (onlineMutes.isEmpty()) {
      audience.sendWarning(TranslatableComponent.of("moderation.mute.none"));
      return;
    }

    Component names = TextComponent.join(TextComponent.of(", ", TextColor.GRAY), onlineMutes);
    Component message =
        TextComponent.builder()
            .append(TranslatableComponent.of("moderation.mute.list", TextColor.GOLD))
            .append("(", TextColor.GRAY)
            .append(Integer.toString(onlineMutes.size()), TextColor.YELLOW)
            .append("): ", TextColor.GRAY)
            .append(names)
            .build();
    audience.sendMessage(message);
  }
}

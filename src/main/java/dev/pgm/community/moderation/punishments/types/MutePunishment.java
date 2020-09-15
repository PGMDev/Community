package dev.pgm.community.moderation.punishments.types;

import dev.pgm.community.moderation.ModerationConfig;
import dev.pgm.community.moderation.punishments.PunishmentType;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.event.HoverEvent;
import net.kyori.text.format.TextColor;
import tc.oc.pgm.util.chat.Audience;
import tc.oc.pgm.util.text.PeriodFormats;

public class MutePunishment extends ExpirablePunishment {

  public MutePunishment(
      UUID id,
      UUID targetId,
      Optional<UUID> issuerId,
      String reason,
      Instant timeIssued,
      Duration length,
      boolean active,
      Instant lastUpdated,
      Optional<UUID> lastUpdatedBy,
      ModerationConfig config) {
    super(
        id,
        targetId,
        issuerId,
        reason,
        timeIssued,
        length,
        active,
        lastUpdated,
        lastUpdatedBy,
        config);
  }

  // When muted player attempts to chat
  public Component getChatMuteMessage() {
    Component chat =
        TextComponent.builder()
            .append("You are unable to chat while muted: ")
            .append(getReason(), TextColor.RED)
            .color(TextColor.GRAY)
            .build();
    return formatWithHover(chat);
  }

  // When muted player attempts to place sign
  public Component getSignMuteMessage() {
    Component sign =
        TextComponent.builder()
            .append("You are unable to write on signs while muted: ")
            .append(getReason(), TextColor.RED)
            .color(TextColor.GRAY)
            .build();
    return formatWithHover(sign);
  }

  // When player is muted
  public Component getMutedMessage() {
    Component muted =
        TextComponent.builder()
            .append("You have been muted for ")
            .append(getReason(), TextColor.RED)
            .color(TextColor.GRAY)
            .build();
    return formatWithHover(muted);
  }

  // Include expire time on hover
  private Component formatWithHover(Component message) {
    return TextComponent.builder()
        .append(message)
        .hoverEvent(
            HoverEvent.showText(
                TextComponent.builder()
                    .append("Expires in ", TextColor.GRAY)
                    .append(
                        PeriodFormats.briefNaturalApproximate(
                                Duration.between(Instant.now(), getExpireTime()), 1)
                            .color(TextColor.YELLOW))
                    .build()))
        .build();
  }

  @Override
  public boolean punish() {
    getTargetPlayer()
        .map(Audience::get)
        .ifPresent(
            player -> {
              this.sendWarning(player, getReason());
              player.sendWarning(getMutedMessage());
            });
    return getTargetPlayer().isPresent();
  }

  @Override
  public PunishmentType getType() {
    return PunishmentType.MUTE;
  }
}

package dev.pgm.community.moderation.punishments.types;

import static net.kyori.adventure.text.Component.text;

import dev.pgm.community.moderation.ModerationConfig;
import dev.pgm.community.moderation.punishments.PunishmentType;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import tc.oc.pgm.util.Audience;
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
      String service,
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
        service,
        config);
  }

  // When muted player attempts to chat
  public Component getChatMuteMessage() {
    Component chat =
        text("You are unable to chat while muted: ")
            .append(text(getReason(), NamedTextColor.RED))
            .color(NamedTextColor.GRAY);
    return formatWithHover(chat);
  }

  // When muted player attempts to place sign
  public Component getSignMuteMessage() {
    Component sign =
        text("You are unable to write on signs while muted: ")
            .append(text(getReason(), NamedTextColor.RED))
            .color(NamedTextColor.GRAY);
    return formatWithHover(sign);
  }

  // When player is muted
  public Component getMutedMessage() {
    Component muted =
        text("You have been muted for ")
            .append(text(getReason(), NamedTextColor.RED))
            .color(NamedTextColor.GRAY);
    return formatWithHover(muted);
  }

  // Include expire time on hover
  private Component formatWithHover(Component message) {
    return text()
        .append(message)
        .hoverEvent(
            HoverEvent.showText(
                text("Expires in ", NamedTextColor.GRAY)
                    .append(
                        PeriodFormats.briefNaturalApproximate(
                                Duration.between(Instant.now(), getExpireTime()), 1, true)
                            .color(NamedTextColor.YELLOW))))
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

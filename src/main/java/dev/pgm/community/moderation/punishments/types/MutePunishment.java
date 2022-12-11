package dev.pgm.community.moderation.punishments.types;

import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.text;
import static tc.oc.pgm.util.text.TemporalComponent.duration;

import dev.pgm.community.moderation.punishments.PunishmentType;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import tc.oc.pgm.util.Audience;

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
      String service) {
    super(
        PunishmentType.MUTE,
        id,
        targetId,
        issuerId,
        reason,
        timeIssued,
        length,
        active,
        lastUpdated,
        lastUpdatedBy,
        service);
  }

  // When muted player attempts to chat
  public Component getChatMuteMessage() {
    Component chat =
        text("You are muted. Expires in ")
            .append(getExpireTimeComponent())
            .append(text(": "))
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
    Component hover =
        text()
            .append(text("Issued: "))
            .append(text(this.getTimeIssued().toString(), NamedTextColor.YELLOW))
            .append(newline())
            .append(text("Total duration: "))
            .append(duration(this.getDuration(), NamedTextColor.YELLOW))
            .append(newline())
            .append(text("Expires: "))
            .append(getExpireTimeComponent())
            .color(NamedTextColor.GRAY)
            .build();

    return text().append(message).hoverEvent(hover).build();
  }

  private Component getExpireTimeComponent() {
    return duration(Duration.between(Instant.now(), getExpireTime()), NamedTextColor.YELLOW);
  }

  @Override
  public boolean punish(boolean silent) {
    getTargetPlayer()
        .map(Audience::get)
        .ifPresent(
            player -> {
              this.sendWarning(player, getReason());
              player.sendWarning(getMutedMessage());
            });
    return getTargetPlayer().isPresent();
  }
}

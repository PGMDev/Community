package dev.pgm.community.moderation.punishments.types;

import dev.pgm.community.moderation.ModerationConfig;
import dev.pgm.community.moderation.punishments.PunishmentType;
import dev.pgm.community.usernames.UsernameService;
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
      ModerationConfig config,
      UsernameService usernames) {
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
        config,
        usernames);
  }

  public Component getMuteMessage() {
    return TextComponent.builder()
        .append("You have been muted for ", TextColor.GRAY)
        .append(getReason(), TextColor.RED)
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
              player.sendWarning(getMuteMessage());
            });
    return getTargetPlayer().isPresent();
  }

  @Override
  public PunishmentType getType() {
    return PunishmentType.MUTE;
  }
}

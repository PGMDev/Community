package dev.pgm.community.moderation.punishments;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

import com.google.common.collect.Lists;
import dev.pgm.community.Community;
import dev.pgm.community.moderation.ModerationConfig;
import dev.pgm.community.moderation.punishments.types.BanPunishment;
import dev.pgm.community.moderation.punishments.types.ExpirablePunishment;
import dev.pgm.community.moderation.punishments.types.KickPunishment;
import dev.pgm.community.moderation.punishments.types.MutePunishment;
import dev.pgm.community.moderation.punishments.types.TempBanPunishment;
import dev.pgm.community.moderation.punishments.types.WarnPunishment;
import dev.pgm.community.utils.BroadcastUtils;
import dev.pgm.community.utils.MessageUtils;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import tc.oc.pgm.util.text.PeriodFormats;
import tc.oc.pgm.util.text.TextTranslations;

public interface Punishment {

  /**
   * Get the the Punishment ID A Unique id is assigned to each punishment
   *
   * @return The punishment id
   */
  UUID getId();

  /**
   * Get the target player's UUID
   *
   * @return The target {@link UUID}
   */
  UUID getTargetId();

  /**
   * Get an optional UUID of the punishment issuer Note: If punishment is issued by console, will be
   * empty
   *
   * @return A optional {@link UUID}
   */
  Optional<UUID> getIssuerId();

  /**
   * Get the reason for the punishment
   *
   * @return punishment reason
   */
  String getReason();

  /**
   * Get the time punishment was issued
   *
   * @return {@link Instant} punishment was issued
   */
  Instant getTimeIssued();

  /**
   * Perform the punishment on online player
   *
   * @return true if target was online, false if not
   */
  boolean punish();

  /**
   * Get whether the punishment is active Active punishments (mute/ban) rely on checking this for
   * certain functionality. This has no effect on one-time punishments (kick/warn)
   *
   * @return true if punishment is active, false if not
   */
  boolean isActive();

  /**
   * Get the time punishment was last updated (i.e. player pardon)
   *
   * @return {@link Instant} punishment was last updated
   */
  Instant getLastUpdated();

  /**
   * Get who updated the punishment last Note: Optional will be empty if console
   *
   * @return An optional {@link UUID}
   */
  Optional<UUID> getLastUpdatedBy();

  /**
   * Get the type of punishment
   *
   * @return {@link PunishmentType} of punishment
   */
  PunishmentType getType();

  /**
   * Get the name of the service punishment was issued under
   *
   * @return The name of punishment service
   */
  String getService();

  default boolean wasUpdated() {
    return !getTimeIssued().equals(getLastUpdated());
  }

  static Punishment of(
      UUID id,
      UUID target,
      Optional<UUID> issuer,
      String reason,
      Instant time,
      @Nullable Duration length,
      PunishmentType type,
      boolean active,
      Instant lastUpdated,
      Optional<UUID> lastUpdatedBy,
      String service,
      ModerationConfig config) {
    switch (type) {
      case WARN:
        return new WarnPunishment(
            id, target, issuer, reason, time, active, lastUpdated, lastUpdatedBy, service, config);
      case MUTE:
        return new MutePunishment(
            id,
            target,
            issuer,
            reason,
            time,
            length,
            active,
            lastUpdated,
            lastUpdatedBy,
            service,
            config);
      case KICK:
        return new KickPunishment(
            id, target, issuer, reason, time, active, lastUpdated, lastUpdatedBy, service, config);
      case TEMP_BAN:
        return new TempBanPunishment(
            id,
            target,
            issuer,
            reason,
            time,
            length,
            active,
            lastUpdated,
            lastUpdatedBy,
            service,
            config);
      case BAN:
        return new BanPunishment(
            id, target, issuer, reason, time, active, lastUpdated, lastUpdatedBy, service, config);
    }
    return null;
  }

  default Component formatBroadcast(Component issuer, Component target) {
    Component prefix = getType().getPunishmentPrefix();
    if (this instanceof ExpirablePunishment
        && !((ExpirablePunishment) this).getDuration().isZero()) {
      Duration length = ((ExpirablePunishment) this).getDuration();
      String time =
          TextTranslations.translateLegacy(
              PeriodFormats.briefNaturalApproximate(Duration.ofSeconds(length.getSeconds())), null);
      prefix =
          getType()
              .getPunishmentPrefix(
                  time.lastIndexOf('s') != -1
                      ? text(time.substring(0, time.lastIndexOf('s')), NamedTextColor.GOLD)
                      : empty());
    }

    return issuer
        .append(BroadcastUtils.BROADCAST_DIV)
        .append(prefix)
        .append(BroadcastUtils.BROADCAST_DIV)
        .append(target)
        .append(BroadcastUtils.BROADCAST_DIV)
        .append(text(getReason(), NamedTextColor.RED));
  }

  /** Formats a string for multi-line kick message */
  default String formatPunishmentScreen(ModerationConfig config, Component issuerName) {
    List<Component> lines = Lists.newArrayList();

    lines.add(empty());
    lines.add(getType().getScreenComponent(text(getReason(), NamedTextColor.RED)));
    lines.add(empty());

    // If punishment expires, display when
    if (this instanceof ExpirablePunishment) {
      Duration banLength = ((ExpirablePunishment) this).getDuration();
      Duration timeSince = Duration.between(getTimeIssued(), Instant.now());

      Duration remaining = banLength.minus(timeSince);

      Component timeLeft = PeriodFormats.briefNaturalApproximate(remaining);
      lines.add(translatable("moderation.screen.expires", NamedTextColor.GRAY, timeLeft));
      lines.add(empty());
    }

    // Staff Sign-off
    lines.add(translatable("moderation.screen.signoff", NamedTextColor.GRAY, issuerName));

    // Link to rules for review by player
    if (config.getRulesLink() != null && !config.getRulesLink().isEmpty()) {
      Component rules = text(config.getRulesLink(), NamedTextColor.AQUA);

      lines.add(empty());
      lines.add(
          translatable("moderation.screen.rulesLink", NamedTextColor.GRAY, rules)); // Link to rules
    }

    // Configurable last line (for appeal message or etc)
    if (config.getAppealMessage() != null
        && !config.getAppealMessage().isEmpty()
        && getType().equals(PunishmentType.BAN)) {
      lines.add(empty());
      lines.add(text(config.getAppealMessage()));
    }

    lines.add(empty());

    return MessageUtils.formatKickScreenMessage(Community.get().getServerName(), lines);
  }
}

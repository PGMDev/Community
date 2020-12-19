package dev.pgm.community.moderation.punishments;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static net.kyori.adventure.title.Title.title;

import dev.pgm.community.moderation.ModerationConfig;
import dev.pgm.community.utils.MessageUtils;
import dev.pgm.community.utils.Sounds;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title.Times;
import net.kyori.adventure.util.Ticks;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.types.PlayerComponent;

public abstract class PunishmentBase implements Punishment, Comparable<Punishment> {

  private UUID punishmentId;
  private UUID targetId;
  private Optional<UUID> issuerId;
  private String reason;
  private Instant timeIssued;
  private boolean active;

  private Instant lastUpdated;
  private Optional<UUID> lastUpdatedBy;

  private String service;

  private final ModerationConfig config;

  public PunishmentBase(
      UUID punishmentId,
      UUID targetId,
      Optional<UUID> issuerId,
      String reason,
      Instant timeIssued,
      boolean active,
      Instant lastUpdated,
      Optional<UUID> lastUpdatedBy,
      String service,
      ModerationConfig config) {
    this.punishmentId = punishmentId;
    this.targetId = targetId;
    this.issuerId = issuerId;
    this.reason = reason;
    this.timeIssued = timeIssued;
    this.active = active;
    this.config = config;
    this.lastUpdated = lastUpdated;
    this.lastUpdatedBy = lastUpdatedBy;
    this.service = service;
  }

  @Override
  public UUID getId() {
    return punishmentId;
  }

  @Override
  public UUID getTargetId() {
    return targetId;
  }

  @Override
  public Optional<UUID> getIssuerId() {
    return issuerId;
  }

  @Override
  public String getReason() {
    return reason;
  }

  @Override
  public Instant getTimeIssued() {
    return timeIssued;
  }

  @Override
  public boolean isActive() {
    return active;
  }

  @Override
  public Instant getLastUpdated() {
    return lastUpdated;
  }

  @Override
  public Optional<UUID> getLastUpdatedBy() {
    return lastUpdatedBy;
  }

  @Override
  public String getService() {
    return service;
  }

  @Override
  public int compareTo(Punishment o) {
    return -getTimeIssued().compareTo(o.getTimeIssued());
  }

  public ModerationConfig getConfig() {
    return config;
  }

  public Optional<Player> getTargetPlayer() {
    return Optional.ofNullable(Bukkit.getPlayer(getTargetId()));
  }

  public boolean kick() {
    Optional<Player> player = getTargetPlayer();
    if (player.isPresent()) {
      player
          .get()
          .getPlayer()
          .kickPlayer(
              formatPunishmentScreen(
                  config,
                  getIssuerId().isPresent()
                      ? PlayerComponent.player(getIssuerId().get(), NameStyle.FANCY)
                      : MessageUtils.CONSOLE));
      return true;
    }
    return false;
  }

  private static final Component WARN_SYMBOL = text(" \u26a0 ", NamedTextColor.YELLOW);

  /*
   * Sends a formatted title and plays a sound warning a user of their actions
   */
  public void sendWarning(Audience target, String reason) {
    Component titleWord = translatable("misc.warning", NamedTextColor.DARK_RED);
    Component title = text().append(WARN_SYMBOL).append(titleWord).append(WARN_SYMBOL).build();
    Component subtitle = text(reason, NamedTextColor.GOLD);

    target.showTitle(
        title(
            title, subtitle, Times.of(Ticks.duration(5), Ticks.duration(200), Ticks.duration(10))));
    target.playSound(Sounds.WARN_SOUND);
  }
}

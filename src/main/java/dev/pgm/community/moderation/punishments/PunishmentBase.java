package dev.pgm.community.moderation.punishments;

import dev.pgm.community.moderation.ModerationConfig;
import dev.pgm.community.users.feature.UsersFeature;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import tc.oc.pgm.util.chat.Audience;
import tc.oc.pgm.util.chat.Sound;

public abstract class PunishmentBase implements Punishment, Comparable<Punishment> {

  private UUID punishmentId;
  private UUID targetId;
  private Optional<UUID> issuerId;
  private String reason;
  private Instant timeIssued;
  private boolean active;

  private Instant lastUpdated;
  private Optional<UUID> lastUpdatedBy;

  private final ModerationConfig config;
  private final UsersFeature usernames;

  public PunishmentBase(
      UUID punishmentId,
      UUID targetId,
      Optional<UUID> issuerId,
      String reason,
      Instant timeIssued,
      boolean active,
      Instant lastUpdated,
      Optional<UUID> lastUpdatedBy,
      ModerationConfig config,
      UsersFeature usernames) {
    this.punishmentId = punishmentId;
    this.targetId = targetId;
    this.issuerId = issuerId;
    this.reason = reason;
    this.timeIssued = timeIssued;
    this.active = active;
    this.config = config;
    this.usernames = usernames;
    this.lastUpdated = lastUpdated;
    this.lastUpdatedBy = lastUpdatedBy;
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
      usernames
          .renderUsername(getIssuerId())
          .thenAccept(
              issuer -> {
                player.get().getPlayer().kickPlayer(formatPunishmentScreen(config, issuer));
              });
      return true;
    }
    return false;
  }

  private static final Sound WARN_SOUND = new Sound("mob.enderdragon.growl", 1f, 1f);
  private static final Component WARN_SYMBOL = TextComponent.of(" \u26a0 ", TextColor.YELLOW);

  /*
   * Sends a formatted title and plays a sound warning a user of their actions
   */
  public void sendWarning(Audience target, String reason) {
    Component titleWord = TranslatableComponent.of("misc.warning", TextColor.DARK_RED);
    Component title =
        TextComponent.builder().append(WARN_SYMBOL).append(titleWord).append(WARN_SYMBOL).build();
    Component subtitle = TextComponent.of(reason, TextColor.GOLD);

    target.showTitle(title, subtitle, 5, 200, 10);
    target.playSound(WARN_SOUND);
  }
}

package dev.pgm.community.moderation.feature;

import static net.kyori.adventure.text.Component.text;
import static tc.oc.pgm.util.text.TemporalComponent.duration;

import dev.pgm.community.Community;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.moderation.punishments.Punishment;
import dev.pgm.community.moderation.punishments.types.MutePunishment;
import dev.pgm.community.moderation.tools.ModerationTools;
import java.time.Duration;
import java.time.Instant;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import tc.oc.pgm.api.integration.Integration;
import tc.oc.pgm.api.integration.PunishmentIntegration;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.ObserverInteractEvent;
import tc.oc.pgm.events.PlayerParticipationStartEvent;
import tc.oc.pgm.spawns.events.ObserverKitApplyEvent;

public class PGMPunishmentIntegration implements PunishmentIntegration, Listener {

  private ModerationFeatureBase moderation;

  private ModerationTools tools;

  public PGMPunishmentIntegration(ModerationFeatureBase moderation) {
    this.moderation = moderation;
    this.tools = new ModerationTools(moderation.getModerationConfig());
  }

  public void enable() {
    Integration.setPunishmentIntegration(this);
    Community.get().registerListener(this);
  }

  public ModerationTools getTools() {
    return tools;
  }

  @Override
  public boolean isMuted(Player player) {
    return moderation.getCachedMute(player.getUniqueId()).isPresent();
  }

  @Override
  public String getMuteReason(Player player) {
    return moderation
        .getCachedMute(player.getUniqueId())
        .map(MutePunishment::getReason)
        .orElse(null);
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void giveTools(ObserverKitApplyEvent event) {
    if (event.getPlayer().getBukkit().hasPermission(CommunityPermissions.STAFF)) {
      tools.giveTools(event.getPlayer().getBukkit());
    }
  }

  @EventHandler(priority = EventPriority.LOW)
  public void onInteractEvent(ObserverInteractEvent event) {
    if (event.getPlayer().getBukkit().hasPermission(CommunityPermissions.STAFF)) {
      tools.onInteract(event);
    }
  }

  @EventHandler
  public void onMatchJoin(PlayerParticipationStartEvent event) {
    MatchPlayer player = event.getPlayer();
    if (moderation.getMatchBans() != null) {
      Punishment punishment = moderation.getMatchBans().getIfPresent(player.getId());

      if (punishment != null) {
        Duration elasped = Duration.between(punishment.getTimeIssued(), Instant.now());
        Duration remaining = moderation.getModerationConfig().getMatchBanDuration().minus(elasped);
        Component reason =
            text()
                .append(text("You are banned from this match for "))
                .append(duration(remaining, NamedTextColor.YELLOW))
                .hoverEvent(
                    HoverEvent.showText(
                        text()
                            .append(text("Reason: ", NamedTextColor.AQUA))
                            .append(text(punishment.getReason(), NamedTextColor.GRAY))))
                .build();
        event.cancel(reason);
      }
    }
  }
}

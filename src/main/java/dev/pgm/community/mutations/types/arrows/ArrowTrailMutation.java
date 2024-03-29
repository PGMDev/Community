package dev.pgm.community.mutations.types.arrows;

import dev.pgm.community.mutations.Mutation;
import dev.pgm.community.mutations.MutationBase;
import dev.pgm.community.mutations.MutationType;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.projectiles.ProjectileSource;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.api.setting.SettingValue;
import tc.oc.pgm.util.bukkit.MetadataUtils;

public class ArrowTrailMutation extends MutationBase {

  private static final String TRAIL_META = "projectile_trail_color";
  private static final String CRITICAL_META = "arrow_is_critical";

  private ScheduledFuture<?> task;

  public ArrowTrailMutation(Match match) {
    super(match, MutationType.ARROW_TRAIL);
  }

  @Override
  public boolean canEnable(Set<Mutation> existing) {
    return true;
  }

  @Override
  public void enable() {
    super.enable();
    task =
        match
            .getExecutor(MatchScope.RUNNING)
            .scheduleAtFixedRate(this::checkMatchProjectiles, 0l, 50, TimeUnit.MILLISECONDS);
  }

  @Override
  public void disable() {
    task.cancel(true);
    super.disable();
  }

  public void checkMatchProjectiles() {
    match.getWorld().getEntitiesByClass(Projectile.class).stream()
        .filter(projectile -> projectile.hasMetadata(TRAIL_META))
        .forEach(
            projectile -> {
              if (projectile.isDead() || projectile.isOnGround()) {
                projectile.removeMetadata(TRAIL_META, PGM.get());
              } else {
                Color color =
                    projectile.hasMetadata(TRAIL_META)
                        ? (Color)
                            MetadataUtils.getMetadata(projectile, TRAIL_META, PGM.get()).value()
                        : null;

                for (MatchPlayer player : match.getPlayers()) {
                  boolean colors =
                      player
                          .getSettings()
                          .getValue(SettingKey.EFFECTS)
                          .equals(SettingValue.EFFECTS_ON);
                  if (colors) {
                    player
                        .getBukkit()
                        .spigot()
                        .playEffect(
                            projectile.getLocation(),
                            Effect.COLOURED_DUST,
                            0,
                            0,
                            rgbToParticle(color.getRed()),
                            rgbToParticle(color.getGreen()),
                            rgbToParticle(color.getBlue()),
                            1,
                            0,
                            50);
                  } else {
                    // Play the critical effect to those who have effects off, to replicate original
                    // arrow behavior
                    if (isCriticalArrow(projectile)) {
                      player
                          .getBukkit()
                          .spigot()
                          .playEffect(
                              projectile.getLocation(), Effect.CRIT, 0, 0, 0, 0, 0, 1, 0, 50);
                    }
                  }
                }
              }
            });
  }

  private float rgbToParticle(int rgb) {
    return Math.max(0.001f, (rgb / 255.0f));
  }

  private boolean isCriticalArrow(Projectile projectile) {
    if (projectile instanceof Arrow) {
      final Arrow arrow = (Arrow) projectile;
      if (arrow.hasMetadata(CRITICAL_META)) {
        return MetadataUtils.getMetadata(projectile, CRITICAL_META, PGM.get()).asBoolean();
      }
    }
    return false;
  }

  static Player getShooter(Projectile projectile) {
    ProjectileSource shooter = projectile.getShooter();
    return shooter instanceof Player ? (Player) shooter : null;
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onProjectileLaunch(ProjectileLaunchEvent event) {
    MatchPlayer player = match.getPlayer(getShooter(event.getEntity()));
    if (player != null) {
      final Projectile projectile = event.getEntity();
      projectile.setMetadata(
          TRAIL_META, new FixedMetadataValue(PGM.get(), player.getParty().getFullColor()));
      // Set critical metadata to false in order to remove default particle trail.
      // The metadata will be restored just before the arrow hits something.
      if (projectile instanceof Arrow) {
        final Arrow arrow = (Arrow) projectile;
        arrow.setMetadata(CRITICAL_META, new FixedMetadataValue(PGM.get(), arrow.isCritical()));
        arrow.setCritical(false);
      }
    }
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onProjectileHit(ProjectileHitEvent event) {
    MatchPlayer player = match.getPlayer(getShooter(event.getEntity()));
    if (player != null) {
      final Projectile projectile = event.getEntity();
      projectile.removeMetadata(TRAIL_META, PGM.get());
      // Restore critical metadata to arrows if applicable
      if (projectile instanceof Arrow) {
        final Arrow arrow = (Arrow) projectile;
        if (arrow.hasMetadata(CRITICAL_META)) {
          arrow.setCritical(MetadataUtils.getMetadata(arrow, CRITICAL_META, PGM.get()).asBoolean());
        }
      }
    }
  }
}

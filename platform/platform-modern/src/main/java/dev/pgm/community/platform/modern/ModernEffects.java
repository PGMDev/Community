package dev.pgm.community.platform.modern;

import static dev.pgm.community.util.Supports.Variant.PAPER;

import dev.pgm.community.util.Effects;
import dev.pgm.community.util.Supports;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;

@Supports(value = PAPER, minVersion = "1.20.6")
public class ModernEffects implements Effects {
  @Override
  public void dummy() {}

  @Override
  public void tntRainExplode(TNTPrimed tnt) {
    tnt.getWorld().spawnParticle(Particle.LAVA, tnt.getLocation(), 10, 1);
  }

  @Override
  public void mobSpawnEffect(Location loc) {
    loc.getWorld().spawnParticle(Particle.FLAME, loc, 10, 1);
  }

  @Override
  public void explosionEffect(Location loc) {
    loc.getWorld().spawnParticle(Particle.LAVA, loc, 15);
  }

  @Override
  public void batTakeoffSound(Player player) {
    player.playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 2, 1.2f);
  }
}

package dev.pgm.community.platform.sportpaper;

import static dev.pgm.community.util.Supports.Variant.SPORTPAPER;

import dev.pgm.community.util.Effects;
import dev.pgm.community.util.Supports;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;

@Supports(SPORTPAPER)
public class SpEffects implements Effects {
  @Override
  public void dummy() {}

  @Override
  public void tntRainExplode(TNTPrimed tnt) {
    tnt.getWorld()
        .spigot()
        .playEffect(tnt.getLocation(), Effect.LAVA_POP, 0, 0, 0, 0, 0, 1, 10, 50);
  }

  @Override
  public void mobSpawnEffect(Location loc) {
    loc.getWorld().spigot().playEffect(loc, Effect.FLAME, 0, 0, 0, 0, 0, 0, 5, 100);
  }

  @Override
  public void explosionEffect(Location loc) {
    loc.getWorld().spigot().playEffect(loc, Effect.LAVA_POP, 0, 0, 0, 0, 0, 0, 15, 50);
  }

  @Override
  public void batTakeoffSound(Player player) {
    player.playSound(player.getLocation(), Sound.BAT_TAKEOFF, 2, 1.2f);
  }
}

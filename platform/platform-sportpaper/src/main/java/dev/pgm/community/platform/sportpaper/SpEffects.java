package dev.pgm.community.platform.sportpaper;

import static tc.oc.pgm.util.platform.Supports.Priority.HIGH;
import static tc.oc.pgm.util.platform.Supports.Variant.SPORTPAPER;

import dev.pgm.community.platform.CommunityEffects;
import org.bukkit.Effect;
import org.bukkit.Location;
import tc.oc.pgm.util.platform.Supports;

@Supports(value = SPORTPAPER, priority = HIGH)
public class SpEffects implements CommunityEffects {
  @Override
  public void tntRainEffect(Location location) {
    location.getWorld().spigot().playEffect(location, Effect.LAVA_POP, 0, 0, 0, 0, 0, 1, 10, 50);
  }

  @Override
  public void explosionEffect(Location location) {
    location.getWorld().spigot().playEffect(location, Effect.LAVA_POP, 0, 0, 0, 0, 0, 0, 15, 50);
  }

  @Override
  public void mobFlameEffect(Location location) {
    location.getWorld().spigot().playEffect(location, Effect.FLAME, 0, 0, 0, 0, 0, 0, 5, 100);
  }
}

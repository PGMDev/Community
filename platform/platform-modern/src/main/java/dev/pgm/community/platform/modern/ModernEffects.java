package dev.pgm.community.platform.modern;

import static tc.oc.pgm.util.platform.Supports.Variant.PAPER;

import dev.pgm.community.platform.CommunityEffects;
import org.bukkit.Location;
import tc.oc.pgm.util.platform.Supports;

@Supports(value = PAPER, minVersion = "1.20.6")
public class ModernEffects implements CommunityEffects {
  @Override
  public void tntRainEffect(Location location) {}

  @Override
  public void explosionEffect(Location location) {}

  @Override
  public void mobFlameEffect(Location location) {}
}

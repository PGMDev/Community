package dev.pgm.community.platform;

import static dev.pgm.community.platform.Reflections.INSTANCE;

import org.bukkit.Location;
import tc.oc.pgm.util.platform.Platform;

public interface CommunityEffects {
  CommunityEffects EFFECTS = Platform.get(CommunityEffects.class, INSTANCE);

  void tntRainEffect(Location location);

  void explosionEffect(Location location);

  void mobFlameEffect(Location location);
}

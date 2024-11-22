package dev.pgm.community.util;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;

public interface Effects {
  Effects EFFECTS = Platform.get(Effects.class);

  void dummy();

  void tntRainExplode(TNTPrimed tnt);

  void mobSpawnEffect(Location loc);

  void explosionEffect(Location loc);

  void batTakeoffSound(Player player);
}

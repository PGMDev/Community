package dev.pgm.community.util;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

public interface EntityUtils {
  EntityUtils ENTITY_UTILS = Platform.get(EntityUtils.class);

  void follow(LivingEntity mob, Location location, float speed);
}

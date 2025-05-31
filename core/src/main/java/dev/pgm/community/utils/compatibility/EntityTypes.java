package dev.pgm.community.utils.compatibility;

import org.bukkit.entity.EntityType;
import tc.oc.pgm.util.bukkit.BukkitUtils;

public interface EntityTypes {
  EntityType PIG_ZOMBIE = parse("PIG_ZOMBIE", "ZOMBIFIED_PIGLIN");

  private static EntityType parse(String... names) {
    return BukkitUtils.parse(EntityType::valueOf, names);
  }
}

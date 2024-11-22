package dev.pgm.community.util;

import org.bukkit.attribute.Attribute;
import tc.oc.pgm.util.bukkit.BukkitUtils;

public class Attributes {

  public static Attribute MOVEMENT_SPEED = parse("GENERIC_MOVEMENT_SPEED", "MOVEMENT_SPEED");
  public static Attribute KNOCKBACK_RESISTANCE =
      parse("GENERIC_KNOCKBACK_RESISTANCE", "KNOCKBACK_RESISTANCE");

  private static Attribute parse(String... names) {
    Attribute type = BukkitUtils.parse(Attribute::valueOf, names);
    return type;
  }
}

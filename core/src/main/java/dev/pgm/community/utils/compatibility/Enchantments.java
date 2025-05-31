package dev.pgm.community.utils.compatibility;

import org.bukkit.enchantments.Enchantment;
import tc.oc.pgm.util.bukkit.BukkitUtils;

public interface Enchantments {
  Enchantment INFINITY = parse("ARROW_INFINITE", "infinity");
  Enchantment LUCK_OF_THE_SEA = parse("LUCK", "luck_of_the_sea");

  private static Enchantment parse(String... names) {
    return BukkitUtils.parse(Enchantment::getByName, names);
  }
}

package dev.pgm.community.utils.compatibility;

import org.bukkit.potion.PotionEffectType;
import tc.oc.pgm.util.bukkit.BukkitUtils;

public interface PotionEffects {
  PotionEffectType NAUSEA = parse("CONFUSION", "nausea");
  PotionEffectType RESISTANCE = parse("DAMAGE_RESISTANCE", "resistance");
  PotionEffectType HASTE = parse("FAST_DIGGING", "HASTE");
  PotionEffectType STRENGTH = parse("INCREASE_DAMAGE", "strength");
  PotionEffectType JUMP_BOOST = parse("JUMP", "jump_boost");
  PotionEffectType SLOWNESS = parse("SLOW", "slowness");
  PotionEffectType MINING_FATIGUE = parse("SLOW_DIGGING", "mining_fatigue");

  private static PotionEffectType parse(String... names) {
    return BukkitUtils.parse(PotionEffectType::getByName, names);
  }
}

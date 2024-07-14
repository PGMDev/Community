package dev.pgm.community.utils.platform;

import org.bukkit.Material;
import tc.oc.pgm.util.bukkit.BukkitUtils;

public interface CommunityMaterials {

  // Constants used across Community for materials that changed name in newer versions
  Material RED_ROSE = parse("RED_ROSE", "POPPY");
  Material GUN_POWDER = parse("SULFUR", "GUNPOWDER");
  Material MOB_SPAWNER = parse("MOB_SPAWNER", "SPAWNER");
  Material FIREBALL = parse("FIREBALL", "FIRE_CHARGE");
  Material GOLD_PLATE = parse("GOLD_PLATE", "LIGHT_WEIGHTED_PRESSURE_PLATE");
  Material WOOD_PLATE = parse("WOOD_PLATE", "OAK_PRESSURE_PLATE");
  Material IRON_BARDING = parse("IRON_BARDING", "IRON_HORSE_ARMOR");
  Material BANNER = parse("BANNER", "WHITE_BANNER");
  Material TRAP_DOOR = parse("TRAP_DOOR", "OAK_TRAP_DOOR");
  Material BED = parse("BED", "RED_BED");
  Material RAILS = parse("RAILS", "RAIL");
  Material REDSTONE_COMPARATOR = parse("REDSTONE_COMPARATOR", "COMPARATOR");
  Material EMPTY_MAP = parse("EMPTY_MAP", "MAP");
  Material WOOD_PICKAXE = parse("WOOD_PICKAXE", "WOODEN_PICKAXE");
  Material WOOD_SWORD = parse("WOOD_SWORD", "WOODEN_SWORD");
  Material NETHER_BRICK_ITEM = parse("NETHER_BRICK_ITEM", "NETHER_BRICK");
  Material REDSTONE_TORCH_ON = parse("REDSTONE_TORCH_ON", "REDSTONE_TORCH");
  Material FENCE = parse("FENCE", "OAK_FENCE");
  Material WOOD_STEP = parse("WOOD_STEP", "OAK_SLAB");
  Material WOOD = parse("WOOD", "OAK_PLANKS");
  Material DIODE = parse("DIODE", "REPEATER");

  static Material parse(String... names) {
    return BukkitUtils.parse(Material::valueOf, names);
  }
}

package dev.pgm.community.utils.compatibility;

import org.bukkit.Material;
import tc.oc.pgm.util.bukkit.BukkitUtils;

public interface Materials {

  Material WOOL = parse("WOOL", "LEGACY_WOOL");
  Material WEB = parse("WEB", "COBWEB");
  Material SIGN = parse("SIGN", "OAK_SIGN");
  Material BOOK_AND_QUILL = parse("BOOK_AND_QUILL", "WRITABLE_BOOK");
  Material EYE_OF_ENDER = parse("EYE_OF_ENDER", "ENDER_EYE");
  Material FIREWORK = parse("FIREWORK", "FIREWORK_ROCKET");
  Material WATCH = parse("WATCH", "CLOCK");
  Material DYE = parse("INK_SACK", "BLACK_DYE");
  Material WORKBENCH = parse("WORKBENCH", "CRAFTING_TABLE");
  Material IRON_BARDING = parse("IRON_BARDING", "IRON_HORSE_ARMOR");
  Material BANNER = parse("BANNER", "WHITE_BANNER");
  Material TRAP_DOOR = parse("TRAP_DOOR", "OAK_TRAPDOOR");
  Material BED = parse("BED", "RED_BED");
  Material GRASS = parse("GRASS", "GRASS_BLOCK");
  Material GOLD_PLATE = parse("GOLD_PLATE", "HEAVY_WEIGHTED_PRESSURE_PLATE");
  Material WOOD_PLATE = parse("WOOD_PLATE", "OAK_PRESSURE_PLATE");
  Material FENCE = parse("FENCE", "OAK_FENCE");
  Material WOOD_STEP = parse("WOOD_STEP", "OAK_SLAB");
  Material WOOD = parse("WOOD", "OAK_PLANKS");
  Material DIODE = parse("DIODE", "REPEATER");
  Material SKULL_ITEM = parse("SKULL_ITEM", "PLAYER_HEAD");
  Material NETHER_BRICK_ITEM = parse("NETHER_BRICK_ITEM", "NETHER_BRICK");
  Material FIREBALL = parse("FIREBALL", "FIRE_CHARGE");
  Material REDSTONE_TORCH_ON = parse("REDSTONE_TORCH_ON", "REDSTONE_TORCH");
  Material MOB_SPAWNER = parse("MOB_SPAWNER", "SPAWNER");
  Material RED_ROSE = parse("RED_ROSE", "POPPY");
  Material SULPHUR = parse("SULPHUR", "GUNPOWDER");
  Material STAINED_GLASS_PANE = parse("STAINED_GLASS_PANE", "LEGACY_STAINED_GLASS_PANE");
  Material WOOD_SWORD = parse("WOOD_SWORD", "WOODEN_SWORD");
  Material WOOD_PICKAXE = parse("WOOD_PICKAXE", "WOODEN_PICKAXE");
  Material EMPTY_MAP = parse("EMPTY_MAP", "MAP");
  Material REDSTONE_COMPARATOR = parse("REDSTONE_COMPARATOR", "COMPARATOR");
  Material RAILS = parse("RAILS", "RAIL");

  static Material parse(String... names) {
    return BukkitUtils.parse(Material::valueOf, names);
  }
}

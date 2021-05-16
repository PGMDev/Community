package dev.pgm.community.mutations;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.util.inventory.ItemBuilder;

public enum MutationType {
  RAGE("Rage", "Instant death", Material.BOW),
  BLITZ("Blitz", "A limited number of lives", Material.EGG),
  EXPLOSION("Explosion", "Random explosions when mining blocks", Material.TNT),
  FLY("Fly", "Everyone can fly", Material.FEATHER),
  JUMP("Jump", "Double jump", Material.SLIME_BLOCK),
  FIREWORK("Firework", "Celebrate with random fireworks!", Material.FIREWORK),
  POTION("Potion", "Random potions everywhere", Material.POTION),
  BLIND("Blindness", "Lights out", Material.COAL),
  HEALTH("Health", "Double health", Material.RED_ROSE);

  String displayName;
  String description;
  Material icon;

  MutationType(String displayName, String description, Material icon) {
    this.displayName = displayName;
    this.description = description;
    this.icon = icon;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getDescription() {
    return description;
  }

  public Material getMaterial() {
    return icon;
  }

  public ItemStack getIcon(boolean enabled) {
    ItemBuilder item =
        new ItemBuilder()
            .material(getMaterial())
            .name((enabled ? ChatColor.GREEN : ChatColor.RED) + getDisplayName())
            .lore(ChatColor.GRAY + getDescription())
            .amount(1)
            .flags(ItemFlag.values());

    if (enabled) {
      item.enchant(Enchantment.LUCK, 0);
    }

    return item.build();
  }
}

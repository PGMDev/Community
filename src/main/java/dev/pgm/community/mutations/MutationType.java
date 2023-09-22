package dev.pgm.community.mutations;

import static net.kyori.adventure.text.Component.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.util.inventory.ItemBuilder;

public enum MutationType {
  RAGE("Rage", "Instant death", Material.BOW),
  BLITZ("Blitz", "A limited number of lives", Material.EGG),
  EXPLOSION("Explosion", "TNT, Fireballs, and random explosions", Material.SULPHUR),
  FLY("Fly", "Everyone can fly", Material.FEATHER),
  JUMP("Jump", "Double jump", Material.SLIME_BLOCK),
  FIREWORK("Firework", "Celebrate with fireworks!", Material.FIREWORK),
  POTION("Potion", "Random potions everywhere", Material.POTION),
  BREAD("Bread", "Bread with powerful enchantments or attributes", Material.BREAD),
  BLIND("Blindness", "Lights out", Material.COAL),
  HEALTH("Health", "Double health", Material.RED_ROSE),
  GHOST("Ghost", "Everyone turns invisible", Material.GLASS),
  STORM("Storm", "Stormy weather with lots of lightning", Material.WATER_BUCKET),
  FRIENDLY("Friendly Fire", "Kill whoever you like", Material.ROTTEN_FLESH),
  ARROW_TRAIL("Arrow Trail", "Particle effects follow projectiles", Material.ARROW),
  ENDERPEARL("Enderpearl", "All projectiles are enderpearls", Material.ENDER_PEARL),
  BLOCK_DECAY("Block Decay", "Blocks placed decay after some time", Material.SOUL_SAND),
  KNOCKBACK("Knockback", "Knockback applied to everything", Material.FISHING_ROD),
  WEB_SLINGERS("Web Slingers", "Shoot webs like a spider", Material.WEB),
  MOBS("Mob", "Attack of the mobs", Material.MOB_SPAWNER),
  TNT_BOW("TNT Bow", "All projectiles are TNT", Material.TNT),
  FIREBALL_BOW("Fireball Bow", "All projectiles are fireballs", Material.FIREBALL),
  CANNON_SUPPLIES("Cannon Supplies", "Supplies for making TNT cannons", Material.REDSTONE),
  GRAPPLING_HOOK("Grappling Hook", "Everyone can use a grappling hook", Material.FISHING_ROD),
  NO_SPAWN_KIT("No Spawn Kit", "No Spawn Kits!", Material.BARRIER);

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

  public Component getComponent() {
    return text()
        .append(text(getDisplayName()))
        .hoverEvent(HoverEvent.showText(text(getDescription(), NamedTextColor.GRAY)))
        .build();
  }
}

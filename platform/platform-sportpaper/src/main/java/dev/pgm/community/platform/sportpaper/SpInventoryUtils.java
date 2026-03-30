package dev.pgm.community.platform.sportpaper;

import static dev.pgm.community.util.Supports.Variant.SPORTPAPER;

import dev.pgm.community.util.InventoryUtils;
import dev.pgm.community.util.Supports;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionType;
import tc.oc.pgm.kits.tag.ItemTags;
import tc.oc.pgm.util.bukkit.BukkitUtils;

@Supports(SPORTPAPER)
public class SpInventoryUtils implements InventoryUtils {
  @Override
  public void addAttributeModifier(ItemMeta meta, Attribute attribute, AttributeModifier modifier) {
    meta.addAttributeModifier(attribute, modifier);
  }

  @Override
  public ItemStack getRandomPotion(boolean splash, Random random) {
    List<PotionType> safeTypes = Stream.of(PotionType.values())
        .filter(p -> p != PotionType.WATER) // No water lol
        .toList();
    PotionType randomType = safeTypes.get(random.nextInt(safeTypes.size()));
    Potion potion = new Potion(randomType, 1, splash);
    ItemStack item = new ItemStack(Material.POTION);
    potion.apply(item);
    ItemMeta meta = item.getItemMeta();
    meta.setDisplayName(BukkitUtils.colorize("&d&lMystery Potion"));
    meta.addItemFlags(ItemFlag.values());
    item.setItemMeta(meta);
    ItemTags.PREVENT_SHARING.set(item, true);
    return item;
  }
}

package dev.pgm.community.platform.modern;

import static dev.pgm.community.util.Supports.Variant.PAPER;
import static net.kyori.adventure.text.Component.text;

import dev.pgm.community.util.InventoryUtils;
import dev.pgm.community.util.Supports;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;
import tc.oc.pgm.kits.tag.ItemTags;

@Supports(value = PAPER, minVersion = "1.21.11")
public class ModernInventoryUtils implements InventoryUtils {
  @Override
  public void addAttributeModifier(ItemMeta meta, Attribute attribute, AttributeModifier modifier) {
    meta.addAttributeModifier(attribute, modifier);
  }

  @Override
  public ItemStack getRandomPotion(boolean splash, Random random) {
    List<PotionType> safeTypes = Stream.of(PotionType.values())
        .filter(p -> switch (p) {
          case WATER, MUNDANE, THICK, AWKWARD, WEAVING, OOZING, INFESTED -> false;
          default -> true;
        })
        .toList();
    PotionType randomType = safeTypes.get(random.nextInt(safeTypes.size()));
    ItemStack item = new ItemStack(splash ? Material.SPLASH_POTION : Material.POTION);
    if (item.getItemMeta() instanceof PotionMeta meta) {
      meta.setBasePotionType(randomType);
      item.setItemMeta(meta);
    }

    ItemMeta meta = item.getItemMeta();
    meta.displayName(text("Mystery Potion", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD));
    meta.addItemFlags(ItemFlag.values());
    item.setItemMeta(meta);
    ItemTags.PREVENT_SHARING.set(item, true);
    return item;
  }
}

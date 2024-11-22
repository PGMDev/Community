package dev.pgm.community.util;

import java.util.Random;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public interface InventoryUtils {
  InventoryUtils INVENTORY_UTILS = Platform.get(InventoryUtils.class);

  void addAttributeModifier(ItemMeta meta, Attribute attribute, AttributeModifier modifier);

  ItemStack getRandomPotion(boolean splash, Random random);
}

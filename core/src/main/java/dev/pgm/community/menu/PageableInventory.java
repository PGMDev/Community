package dev.pgm.community.menu;

import static tc.oc.pgm.util.bukkit.BukkitUtils.colorize;

import fr.minuskube.inv.ClickableItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public interface PageableInventory {

  default ClickableItem getNextPageItem(Player player, int nextPage) {
    return getPageItem(player, nextPage, getPageIcon("&e&lNext Page", nextPage + 1));
  }

  default ClickableItem getPrevPageItem(Player player, int prevPage) {
    return getPageItem(player, prevPage, getPageIcon("&e&lPrevious Page", prevPage + 1));
  }

  ClickableItem getPageItem(Player player, int page, ItemStack icon);

  default ItemStack getPageIcon(String text, int page) {
    return getNamedItem(text, Material.ARROW, page);
  }

  default ItemStack getNamedItem(String text, Material material, int amount) {
    ItemStack stack = new ItemStack(material, amount);
    ItemMeta meta = stack.getItemMeta();
    meta.setDisplayName(colorize(text));
    meta.addItemFlags(ItemFlag.values());
    stack.setItemMeta(meta);
    return stack;
  }
}

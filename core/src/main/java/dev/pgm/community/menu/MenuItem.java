package dev.pgm.community.menu;

import static tc.oc.pgm.util.bukkit.BukkitUtils.colorize;

import fr.minuskube.inv.ClickableItem;
import java.util.List;
import java.util.function.Consumer;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.util.inventory.ItemBuilder;

public abstract class MenuItem {

  private final String name;
  private final Material icon;
  private final String[] description;

  public MenuItem(Material icon, String name, List<String> description) {
    this(icon, name, description.toArray(new String[description.size()]));
  }

  public MenuItem(Material icon, String name, String... description) {
    this.name = colorize(name);
    this.description = colorizeList(description);
    this.icon = icon;
  }

  public String getName() {
    return name;
  }

  public ClickableItem getMenuItem(Consumer<InventoryClickEvent> event) {
    return ClickableItem.of(getItemStack(), event);
  }

  public ItemStack getItemStack() {
    return new ItemBuilder()
        .material(icon)
        .name(name)
        .lore(description)
        .flags(ItemFlag.values())
        .build();
  }

  private String[] colorizeList(String... desc) {
    String[] colorArray = new String[desc.length];
    for (int i = 0; i < desc.length; i++) {
      colorArray[i] = colorize(desc[i]);
    }
    return colorArray;
  }
}

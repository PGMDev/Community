package dev.pgm.community.menu;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import java.util.List;
import java.util.function.Consumer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public abstract class CommunityInventoryProvider<T extends MenuItem> implements InventoryProvider {

  private List<T> items;

  public CommunityInventoryProvider(List<T> items) {
    this.items = items;
  }

  @Override
  public void init(Player player, InventoryContents contents) {
    int slot = getStartingSlot(items.size());
    if (items.isEmpty()) {
      contents.set(0, 4, ClickableItem.empty(getNoResultsItem()));
    }
    for (T item : items) {
      contents.set(0, slot, item.getMenuItem(getClickAction(item, player, contents)));
      slot += 2;
    }
  }

  @Override
  public void update(Player player, InventoryContents contents) {}

  public abstract Consumer<InventoryClickEvent> getClickAction(
      T item, Player viewer, InventoryContents contents);

  protected abstract ItemStack getNoResultsItem();

  protected int getStartingSlot(int categorySize) {
    switch (categorySize) {
      case 3:
        return 2;
      case 4:
        return 1;
      default:
        return 0;
    }
  }
}

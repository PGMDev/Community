package dev.pgm.community.moderation.tools;

import static dev.pgm.community.utils.MessageUtils.colorizeList;
import static tc.oc.pgm.util.bukkit.BukkitUtils.colorize;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.player.event.ObserverInteractEvent;
import tc.oc.pgm.util.inventory.ItemBuilder;

public abstract class ToolBase implements Tool {

  private int slot;
  private boolean enabled;

  public ToolBase(int slot, boolean enabled) {
    this.slot = slot;
    this.enabled = enabled;
  }

  public abstract void onLeftClick(ObserverInteractEvent event);

  public abstract void onRightClick(ObserverInteractEvent event);

  @Override
  public void onInteract(ObserverInteractEvent event) {
    if (!enabled) return;
    if (event.getClickedItem() != null) {
      ItemStack clickedItem = event.getClickedItem();
      if (clickedItem.isSimilar(getItem())) {
        if (event.getClickType() == ClickType.RIGHT) {
          onRightClick(event);
        } else {
          onLeftClick(event);
        }
        event.setCancelled(true);
      }
    }
  }

  @Override
  public void give(Player player) {
    if (!enabled) return;
    player.getInventory().setItem(slot, getItem());
  }

  @Override
  public ItemStack getItem() {
    return new ItemBuilder()
        .material(getMaterial())
        .name(colorize(getName()))
        .lore(colorizeList(getLore()).toArray(new String[getLore().size()]))
        .flags(ItemFlag.values())
        .build();
  }
}

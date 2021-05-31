package dev.pgm.community.moderation.tools.buttons;

import fr.minuskube.inv.ClickableItem;
import java.util.List;
import java.util.function.Consumer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public interface ToolButton {

  Player getViewer();

  Material getMaterial();

  int getAmount();

  String getName();

  List<String> getLore();

  ItemStack getIcon();

  Consumer<InventoryClickEvent> getClickEvent();

  default ClickableItem getItem() {
    return ClickableItem.of(getIcon(), getClickEvent());
  }
}

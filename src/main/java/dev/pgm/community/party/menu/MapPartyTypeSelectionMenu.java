package dev.pgm.community.party.menu;

import com.google.common.collect.Lists;
import dev.pgm.community.menu.StaticMenuItem;
import dev.pgm.community.party.MapPartyType;
import dev.pgm.community.party.feature.MapPartyFeature;
import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.content.InventoryContents;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class MapPartyTypeSelectionMenu extends MapPartyMenu {

  private static final String TITLE = "&a&lSelect mode&8:";
  private static final int ROWS = 3;
  private static final boolean HOST_ONLY = false;

  public MapPartyTypeSelectionMenu(MapPartyFeature feature, Player viewer) {
    super(feature, TITLE, ROWS, HOST_ONLY, viewer);
  }

  @Override
  public void init(Player player, InventoryContents contents) {
    int[] SLOTS = {2, 6};

    int index = 0;
    for (MapPartyType type : MapPartyType.values()) {
      contents.set(
          1,
          SLOTS[index++],
          ClickableItem.of(
              getPartyItem(type),
              c -> {
                Bukkit.dispatchCommand(player, "event create " + type.name());
              }));
    }
    this.addBackButton(contents);
  }

  private ItemStack getPartyItem(MapPartyType type) {
    return new StaticMenuItem(
            type.getMaterial(),
            "&a" + type.getName(),
            Lists.newArrayList(
                "&7" + type.getDescription(), "", "&7Click to select &a" + type.getName()))
        .getItemStack();
  }

  @Override
  public void update(Player player, InventoryContents contents) {}
}

package dev.pgm.community.party.menu.maps;

import static tc.oc.pgm.util.bukkit.BukkitUtils.colorize;

import dev.pgm.community.party.feature.MapPartyFeature;
import dev.pgm.community.party.menu.MapPartyMenu;
import dev.pgm.community.party.types.CustomPoolParty;
import dev.pgm.community.party.types.RegularPoolParty;
import dev.pgm.community.utils.PGMUtils;
import dev.pgm.community.utils.SkullUtils;
import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.Pagination;
import fr.minuskube.inv.content.SlotIterator;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.rotation.MapPoolManager;
import tc.oc.pgm.rotation.pools.MapPool;
import tc.oc.pgm.util.inventory.ItemBuilder;

public class MapMenu extends MapPartyMenu {

  private static final String MAP_TITLE = "&6&lMaps";
  private static final String POOL_TITLE = "&6&lPools";
  private static final int ROWS = 6;
  private static final boolean HOST_ONLY = true;

  public MapMenu(MapPartyFeature feature, Player viewer) {
    super(feature, getTitle(feature), ROWS, HOST_ONLY, viewer);
    open();
  }

  private void render(Player player, InventoryContents contents) {
    contents.fillBorders(getBorderItem());

    if (getFeature().getParty() != null && getFeature().getParty() instanceof CustomPoolParty) {
      CustomPoolParty party = (CustomPoolParty) getFeature().getParty();

      List<MapInfo> maps = party.getMaps();
      List<ClickableItem> mapItems =
          maps.stream().map(this::getMapIcon).collect(Collectors.toList());

      Pagination page = contents.pagination();
      page.setItems(mapItems.toArray(new ClickableItem[mapItems.size()]));
      page.setItemsPerPage(27);

      page.addToIterator(contents.newIterator(SlotIterator.Type.HORIZONTAL, 1, 0));

      // No results
      if (mapItems.isEmpty()) {
        contents.set(2, 4, getNoMapsIcon());
      }

      // Previous
      if (!page.isFirst()) {
        contents.set(4, 1, getPrevPageItem(player, page.getPage() - 1));
      }

      // Next
      if (!page.isLast()) {
        contents.set(4, 7, getNextPageItem(player, page.getPage() + 1));
      }

      // Add new map button
      contents.set(
          4,
          4,
          ClickableItem.of(
              SkullUtils.customSkull(
                  ADD_SKIN, "&a&lAdd Map", "&7Click to add a map to the selection"),
              c -> {
                new MapAddMenu(getFeature(), getViewer());
              }));
    } else {
      RegularPoolParty party = (RegularPoolParty) getFeature().getParty();
      MapPoolManager poolManager = PGMUtils.getMapPoolManager();
      if (poolManager != null) {
        List<MapPool> pools = poolManager.getMapPools();

        int index = 1;
        int row = 1;
        for (MapPool pool : pools) {
          contents.set(row, index++, getPoolIcon(pool, party.getMapPool() == pool));
          if (index > 7) {
            index = 1;
            row++;
            if (row > 4) {
              row = 1;
            }
          }
        }
      }
    }

    // Return to party menu
    contents.set(5, 4, getMainMenuIcon());
  }

  @Override
  public void init(Player player, InventoryContents contents) {
    render(player, contents);
  }

  @Override
  public void update(Player player, InventoryContents contents) {
    render(player, contents);
  }

  private ClickableItem getMapIcon(MapInfo map) {
    return ClickableItem.of(
        new ItemBuilder()
            .material(Material.MAP)
            .name(colorize("&6" + map.getName()))
            .lore(colorize("&7Click to remove"))
            .flags(ItemFlag.values())
            .build(),
        c -> {
          Bukkit.dispatchCommand(getViewer(), "event removemap " + map.getName());
        });
  }

  private ClickableItem getPoolIcon(MapPool pool, boolean active) {
    ItemBuilder icon =
        new ItemBuilder()
            .material(Material.MAP)
            .name(colorize("&6" + StringUtils.capitalize(pool.getName())))
            .lore(
                colorize("&7Maps: &b" + pool.getMaps().size()),
                colorize(active ? "&aSelected Pool" : "&7Click to set"))
            .flags(ItemFlag.values());

    if (active) {
      icon.enchant(Enchantment.LUCK, 1);
    }
    return ClickableItem.of(
        icon.build(),
        c -> {
          Bukkit.dispatchCommand(getViewer(), "event setpool " + pool.getName());
        });
  }

  private ClickableItem getNoMapsIcon() {
    return ClickableItem.empty(
        new ItemBuilder()
            .material(Material.STAINED_GLASS_PANE)
            .color(DyeColor.RED)
            .name(colorize("&cNo Maps found"))
            .lore(colorize("&7Add maps using the button below"))
            .flags(ItemFlag.values())
            .build());
  }

  private static String getTitle(MapPartyFeature feature) {
    if (feature.getParty() != null && feature.getParty() instanceof CustomPoolParty) {
      return MAP_TITLE;
    }
    return POOL_TITLE;
  }
}

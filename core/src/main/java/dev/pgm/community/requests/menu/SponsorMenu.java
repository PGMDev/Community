package dev.pgm.community.requests.menu;

import static tc.oc.pgm.util.bukkit.BukkitUtils.colorize;

import dev.pgm.community.menu.MapSelectionMenu;
import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.util.inventory.ItemBuilder;
import tc.oc.pgm.util.text.TextTranslations;

public class SponsorMenu extends MapSelectionMenu {

  private static final String TITLE = "&e&lSponsor Maps";

  public SponsorMenu(List<MapInfo> maps, Player viewer) {
    super(TITLE, maps, viewer);
  }

  @Override
  public SmartInventory getInventory() {
    return getInventory(null);
  }

  @Override
  public ClickableItem getCloseButton() {
    return ClickableItem.of(
        getNamedItem("&cClose", Material.BARRIER, 1), c -> getViewer().closeInventory());
  }

  @Override
  public ClickableItem getBorderItem() {
    return ClickableItem.empty(
        new ItemBuilder()
            .material(Material.STAINED_GLASS_PANE)
            .color(DyeColor.YELLOW)
            .name(" ")
            .flags(ItemFlag.values())
            .build());
  }

  @Override
  public ClickableItem getMapIcon(MapInfo map) {
    return ClickableItem.of(
        getMapItem(map),
        c -> {
          Bukkit.dispatchCommand(getViewer(), "sponsor request " + map.getName());
          getViewer().closeInventory();
        });
  }

  private ItemStack getMapItem(MapInfo map) {
    return new ItemBuilder()
        .material(getMapMaterial(map))
        .name(colorize("&6" + map.getName()))
        .lore(
            colorize("&7Max Players: &e" + map.getMaxPlayers().stream().reduce(0, Integer::sum)),
            colorize(
                "&7Tags: " + TextTranslations.translateLegacy(renderMapTags(map), getViewer())),
            colorize("&7Click to select"))
        .flags(ItemFlag.values())
        .build();
  }
}

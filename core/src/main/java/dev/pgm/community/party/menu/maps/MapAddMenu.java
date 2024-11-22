package dev.pgm.community.party.menu.maps;

import static net.kyori.adventure.text.Component.text;
import static tc.oc.pgm.util.bukkit.BukkitUtils.colorize;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import dev.pgm.community.party.feature.MapPartyFeature;
import dev.pgm.community.party.menu.MapPartyMenu;
import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.Pagination;
import fr.minuskube.inv.content.SlotIterator;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapTag;
import tc.oc.pgm.util.inventory.ItemBuilder;
import tc.oc.pgm.util.text.TextFormatter;
import tc.oc.pgm.util.text.TextTranslations;

// TODO: needs further speed optimizations
public class MapAddMenu extends MapPartyMenu {

  private static final String TITLE = "&6&lAdd Map";
  private static final int ROWS = 6;
  private static final boolean HOST_ONLY = true;

  private List<MapInfo> maps;
  private List<MapTag> tags;

  private int filterIndex = 0;

  private boolean viewAll = true;

  public MapAddMenu(MapPartyFeature feature, Player viewer) {
    super(feature, TITLE, ROWS, HOST_ONLY, viewer);

    this.maps = Lists.newArrayList(PGM.get().getMapLibrary().getMaps());
    this.tags = Lists.newArrayList();

    Set<MapTag> tags = Sets.newHashSet();
    for (MapInfo map : maps) {
      tags.addAll(map.getTags());
    }
    this.tags.addAll(tags);

    open();
  }

  private MapTag getFilterTag() {
    return tags.get(filterIndex);
  }

  private void render(Player player, InventoryContents contents) {
    contents.fillRow(0, getBorderItem());
    contents.fillRow(5, getBorderItem());

    contents.set(0, 2, getAllIcon());
    contents.set(0, 4, getFilterIcon());

    List<ClickableItem> mapItems = getFilteredMapItems();

    Pagination page = contents.pagination();
    page.setItems(mapItems.toArray(new ClickableItem[mapItems.size()]));
    page.setItemsPerPage(36);

    page.addToIterator(contents.newIterator(SlotIterator.Type.HORIZONTAL, 1, 0));

    // No results
    if (mapItems.isEmpty()) {
      contents.set(2, 4, getNoMapsIcon());
    }

    // Previous
    if (!page.isFirst()) {
      contents.set(5, 1, getPrevPageItem(player, page.getPage() - 1));
    }

    // Next
    if (!page.isLast()) {
      contents.set(5, 7, getNextPageItem(player, page.getPage() + 1));
    }

    // Return to party menu
    contents.set(
        5,
        4,
        ClickableItem.of(
            getNamedItem("&7Return to &6Maps", Material.CAKE, 1),
            c -> {
              Bukkit.dispatchCommand(player, "event maps");
            }));
  }

  private ClickableItem getAllIcon() {
    ItemBuilder allItemBuilder =
        new ItemBuilder()
            .material(Material.BOOKSHELF)
            .name(colorize((viewAll ? "&a" : "&c") + "View All"))
            .lore(colorize(viewAll ? "&7Click to filter by map tags" : "&7Click to view all maps"))
            .flags(ItemFlag.values());

    if (viewAll) {
      allItemBuilder.enchant(Enchantment.LUCK, 1);
    }

    return ClickableItem.of(
        allItemBuilder.build(),
        c -> {
          this.viewAll = !viewAll;
          getInventory().open(getViewer(), 0);
        });
  }

  private ClickableItem getFilterIcon() {
    if (viewAll) {
      return this.getBorderItem();
    }

    return ClickableItem.of(
        new ItemBuilder()
            .material(getMapTagMaterial(getFilterTag()))
            .name(TextTranslations.translateLegacy(getFilterTag().getName(), getViewer()))
            .lore(
                colorize("&7Filter: &b" + (filterIndex + 1) + " &7/&3 " + tags.size()),
                colorize(
                    "&7Total Maps: &a" + getFilteredMapItems().size() + " &7/&2 " + maps.size()))
            .enchant(Enchantment.LUCK, 1)
            .flags(ItemFlag.values())
            .build(),
        c -> {
          // Reset page when switching categories
          getInventory().open(getViewer(), 0);

          // Reverse
          if (c.isRightClick()) {
            if (filterIndex == 0) {
              filterIndex = tags.size() - 1;
            } else {
              filterIndex--;
            }
          } else {
            // Forward
            if (filterIndex >= tags.size() - 1) {
              filterIndex = 0;
            } else {
              filterIndex++;
            }
          }
        });
  }

  @Override
  public void init(Player player, InventoryContents contents) {
    render(player, contents);
  }

  @Override
  public void update(Player player, InventoryContents contents) {
    int delay = contents.property("update", 0);
    contents.setProperty("update", delay + 1);
    if (delay >= 5) {
      render(player, contents);
      delay = 0;
    }
  }

  private List<ClickableItem> getMapItems(List<MapInfo> maps) {
    return maps.stream().map(this::getMapIcon).collect(Collectors.toList());
  }

  private List<ClickableItem> getFilteredMapItems() {
    return getMapItems(
        maps.stream()
            .filter(
                map -> {
                  if (viewAll) return true;

                  Collection<MapTag> tags = map.getTags();
                  return tags != null && tags.contains(getFilterTag());
                })
            .collect(Collectors.toList()));
  }

  private ClickableItem getMapIcon(MapInfo map) {
    boolean isAdded = getFeature().getParty().isMapAdded(map);

    Material icon =
        map.getTags().isEmpty()
                || !map.getTags().stream().filter(t -> t.isGamemode()).findAny().isPresent()
            ? Material.MAP
            : getMapTagMaterial(
                map.getTags().stream().filter(tag -> tag.isGamemode()).findAny().get());

    ItemBuilder builder =
        new ItemBuilder()
            .material(icon)
            .name(colorize("&6" + map.getName()))
            .lore(
                colorize(
                    "&7Max Players: &e" + map.getMaxPlayers().stream().reduce(0, Integer::sum)),
                colorize(
                    "&7Tags: " + TextTranslations.translateLegacy(renderMapTags(map), getViewer())),
                colorize(isAdded ? "&cAlready added" : "&aClick to add map")) // TODO:
            .flags(ItemFlag.values());

    if (isAdded) {
      builder.enchant(Enchantment.LUCK, 1);
    }

    return ClickableItem.of(
        builder.build(),
        c -> {
          Bukkit.dispatchCommand(getViewer(), "event addmap " + map.getName());
        });
  }

  private ClickableItem getNoMapsIcon() {
    return ClickableItem.empty(
        new ItemBuilder()
            .material(Material.STAINED_GLASS_PANE)
            .color(DyeColor.RED)
            .name(colorize("&cNo Maps found"))
            .lore(colorize("&7Check &b/maps &7for details"))
            .flags(ItemFlag.values())
            .build());
  }

  private Component renderMapTags(MapInfo map) {
    return TextFormatter.list(
        map.getTags().stream()
            .map(tag -> text(tag.getId(), NamedTextColor.DARK_AQUA))
            .collect(Collectors.toList()),
        NamedTextColor.GRAY);
  }

  private Material getMapTagMaterial(MapTag mapTag) {
    switch (mapTag.getId()) {
      case "2teams":
        return Material.LEATHER;
      case "ffa":
        return Material.DIAMOND_SWORD;
      case "border":
        return Material.IRON_BARDING;
      case "wool":
        return Material.WOOL;
      case "controlpoint":
        return Material.BEACON;
      case "flag":
        return Material.BANNER;
      case "classes":
        return Material.FISHING_ROD;
      case "deathmatch":
        return Material.STONE_SWORD;
      case "monument":
        return Material.DIAMOND_PICKAXE;
      case "4teams":
        return Material.TRAP_DOOR;
      case "timelimit":
        return Material.WATCH;
      case "autotnt":
        return Material.TNT;
      case "core":
        return Material.OBSIDIAN;
      case "blitz":
        return Material.EGG;
      case "scorebox":
        return Material.WEB;
      case "6teams":
        return Material.BED;
      case "rage":
        return Material.BOW;
      case "3teams":
        return Material.WORKBENCH;
      case "terrain":
        return Material.GRASS;
      case "8teams":
        return Material.INK_SACK;
      default:
        return Material.MAP;
    }
  }
}

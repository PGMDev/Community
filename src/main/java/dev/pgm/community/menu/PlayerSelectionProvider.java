package dev.pgm.community.menu;

import static tc.oc.pgm.util.bukkit.BukkitUtils.colorize;

import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.utils.VisibilityUtils;
import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import fr.minuskube.inv.content.Pagination;
import fr.minuskube.inv.content.SlotIterator;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import tc.oc.pgm.util.inventory.ItemBuilder;
import tc.oc.pgm.util.nms.NMSHacks;

public abstract class PlayerSelectionProvider implements InventoryProvider {

  private final Material PAGE_MATERIAL = Material.FLINT;

  public abstract SmartInventory getInventory();

  public abstract Consumer<InventoryClickEvent> getClickEvent(Player target);

  public abstract List<String> getPlayerLore(Player viewer, Player player);

  public void open(Player viewer) {
    getInventory().open(viewer);
  }

  @Override
  public void init(Player player, InventoryContents contents) {
    Pagination pages = contents.pagination();
    pages.setItems(getAllPlayers(player));
    pages.setItemsPerPage(46);

    pages.addToIterator(contents.newIterator(SlotIterator.Type.HORIZONTAL, 0, 0));

    // Previous
    if (!pages.isFirst()) {
      Pagination prev = pages.previous();
      contents.set(5, 3, getPrevPageItem(player, prev.getPage()));
    }

    // Next
    if (!pages.isLast()) {
      Pagination next = pages.next();
      contents.set(5, 5, getNextPageItem(player, next.getPage()));
    }
  }

  @Override
  public void update(Player player, InventoryContents contents) {}

  private final ItemStack getPageIcon(String text, int page) {
    return new ItemBuilder().name(colorize(text)).amount(page).material(PAGE_MATERIAL).build();
  }

  private ClickableItem getNextPageItem(Player player, int nextPage) {
    return getPageItem(player, nextPage, getPageIcon("&e&lNext Page", nextPage));
  }

  private ClickableItem getPrevPageItem(Player player, int nextPage) {
    return getPageItem(player, nextPage, getPageIcon("&e&lPrevious Page", nextPage));
  }

  private ClickableItem getPageItem(Player player, int page, ItemStack icon) {
    return ClickableItem.of(icon, c -> getInventory().open(player, page));
  }

  private Comparator<Player> COMPARE = Comparator.comparing(Player::getName).reversed();

  private ClickableItem[] getAllPlayers(Player viewer) {
    List<Player> online =
        Bukkit.getOnlinePlayers().stream()
            .filter(
                p ->
                    !VisibilityUtils.hasOverride(p)
                        || viewer.hasPermission(CommunityPermissions.OVERRIDE))
            .sorted(COMPARE)
            .collect(Collectors.toList());
    ClickableItem[] items = new ClickableItem[online.size()];
    for (int i = 0; i < online.size(); i++) {
      Player player = online.get(i);
      items[i] = getPlayerItem(viewer, player, getClickEvent(player));
    }
    return items;
  }

  private ClickableItem getPlayerItem(
      Player viewer, Player player, Consumer<InventoryClickEvent> event) {
    return ClickableItem.of(getPlayerHead(viewer, player), event);
  }

  private ItemStack getPlayerHead(Player viewer, Player player) {
    ItemStack head = new ItemStack(Material.SKULL_ITEM, 1, (byte) 3);
    SkullMeta meta = (SkullMeta) head.getItemMeta();
    meta.setDisplayName(player.getDisplayName());
    meta.setLore(getPlayerLore(viewer, player));
    NMSHacks.setSkullMetaOwner(
        meta, player.getName(), player.getUniqueId(), NMSHacks.getPlayerSkin(player));
    head.setItemMeta(meta);
    return head;
  }
}

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
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import tc.oc.pgm.util.nms.NMSHacks;

public abstract class PlayerSelectionProvider implements InventoryProvider {

  private final Material PAGE_MATERIAL = Material.ARROW;

  public abstract SmartInventory getInventory();

  public abstract Consumer<InventoryClickEvent> getClickEvent(Player target);

  public abstract List<String> getPlayerLore(Player viewer, Player player);

  public void open(Player viewer) {
    getInventory().open(viewer);
  }

  @Override
  public void init(Player player, InventoryContents contents) {
    Pagination page = contents.pagination();
    page.setItems(getAllPlayers(player));
    page.setItemsPerPage(45);

    page.addToIterator(contents.newIterator(SlotIterator.Type.HORIZONTAL, 0, 0));

    // Previous
    if (!page.isFirst()) {
      contents.set(5, 0, getPrevPageItem(player, page.getPage() - 1));
    }

    // Next
    if (!page.isLast()) {
      contents.set(5, 8, getNextPageItem(player, page.getPage() + 1));
    }
  }

  @Override
  public void update(Player player, InventoryContents contents) {}

  private final ItemStack getPageIcon(String text, int page) {
    return getNamedItem(text, PAGE_MATERIAL, page);
  }

  private ClickableItem getNextPageItem(Player player, int nextPage) {
    return getPageItem(player, nextPage, getPageIcon("&e&lNext Page", nextPage + 1));
  }

  private ClickableItem getPrevPageItem(Player player, int prevPage) {
    return getPageItem(player, prevPage, getPageIcon("&e&lPrevious Page", prevPage + 1));
  }

  private ClickableItem getPageItem(Player player, int page, ItemStack icon) {
    return ClickableItem.of(icon, c -> getInventory().open(player, page));
  }

  public ItemStack getNamedItem(String text, Material material, int amount) {
    ItemStack stack = new ItemStack(material, amount);
    ItemMeta meta = stack.getItemMeta();
    meta.setDisplayName(colorize(text));
    meta.addItemFlags(ItemFlag.values());
    stack.setItemMeta(meta);
    return stack;
  }

  private Comparator<Player> COMPARE =
      Comparator.comparing(
          Player::getName,
          (p1, p2) -> {
            return p1.compareToIgnoreCase(p2);
          });

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

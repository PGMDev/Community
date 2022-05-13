package dev.pgm.community.party.menu;

import static net.kyori.adventure.text.Component.text;
import static tc.oc.pgm.util.bukkit.BukkitUtils.colorize;

import dev.pgm.community.Community;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.party.feature.MapPartyFeature;
import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.inventory.ItemBuilder;
import tc.oc.pgm.util.nms.NMSHacks;
import tc.oc.pgm.util.skin.Skin;

public abstract class MapPartyMenu implements InventoryProvider {

  protected static final Material BORDER_MATERIAL = Material.STAINED_GLASS_PANE;

  private final String title;
  private final int rows;
  private final Player viewer;
  private final MapPartyFeature feature;
  private final boolean hostOnly;

  private MapPartyMenu parent;

  public MapPartyMenu(
      MapPartyFeature feature, String title, int rows, boolean hostOnly, Player viewer) {
    this.feature = feature;
    this.title = title;
    this.rows = rows;
    this.hostOnly = hostOnly;
    this.viewer = viewer;
  }

  public MapPartyFeature getFeature() {
    return feature;
  }

  public Player getViewer() {
    return viewer;
  }

  public MapPartyMenu getParent() {
    return parent;
  }

  public void open() {
    if (hostOnly && checkMenuAccess()) return;
    getInventory().open(viewer);
  }

  private boolean checkMenuAccess() {
    if (getViewer().hasPermission(CommunityPermissions.PARTY_HOST)
        || getViewer().hasPermission(CommunityPermissions.PARTY_ADMIN)) return false;
    Audience.get(getViewer()).sendWarning(text("Only event hosts may access this menu"));
    return true;
  }

  public void addBackButton(InventoryContents contents) {
    if (parent != null) {
      contents.set(
          rows - 1,
          4,
          ClickableItem.of(
              new ItemBuilder()
                  .material(Material.ARROW)
                  .name(colorize("&ePrevious Page"))
                  .flags(ItemFlag.values())
                  .build(),
              c -> getParent().open()));
    }
  }

  public void open(MapPartyMenu parent) {
    this.parent = parent;
    getInventory(parent.getInventory()).open(viewer);
  }

  public void close() {
    getInventory().close(viewer);
  }

  public SmartInventory getInventory() {
    return getInventory(null);
  }

  public SmartInventory getInventory(SmartInventory parent) {
    SmartInventory.Builder builder =
        SmartInventory.builder()
            .title(colorize(title))
            .provider(this)
            .manager(Community.get().getInventory())
            .size(rows, 9);

    if (parent != null) {
      builder.parent(parent);
    }

    return builder.build();
  }

  protected ClickableItem getBorderItem() {
    return ClickableItem.empty(
        new ItemBuilder()
            .material(BORDER_MATERIAL)
            .color(getBorderColor())
            .name(" ")
            .flags(ItemFlag.values())
            .build());
  }

  protected static final String ADD_SKIN =
      "http://textures.minecraft.net/texture/b056bc1244fcff99344f12aba42ac23fee6ef6e3351d27d273c1572531f";

  protected DyeColor getBorderColor() {
    return getFeature().getParty() == null
        ? DyeColor.WHITE
        : getFeature().getParty().isRunning() ? DyeColor.LIME : DyeColor.YELLOW;
  }

  protected ClickableItem getMainMenuIcon() {
    return ClickableItem.of(
        getNamedItem("&7Return to &dMap Party Manager", Material.CAKE, 1),
        c -> {
          Bukkit.dispatchCommand(getViewer(), "event");
        });
  }

  protected ItemStack getPlayerHead(
      String displayName, String name, List<String> lore, UUID playerId, Skin skin) {
    ItemStack head = new ItemStack(Material.SKULL_ITEM, 1, (byte) 3);
    SkullMeta meta = (SkullMeta) head.getItemMeta();
    meta.setDisplayName(displayName);
    meta.setLore(lore);
    NMSHacks.setSkullMetaOwner(meta, name, playerId, skin);
    head.setItemMeta(meta);
    return head;
  }

  protected ClickableItem getNextPageItem(Player player, int nextPage) {
    return getPageItem(player, nextPage, getPageIcon("&e&lNext Page", nextPage + 1));
  }

  protected ClickableItem getPrevPageItem(Player player, int prevPage) {
    return getPageItem(player, prevPage, getPageIcon("&e&lPrevious Page", prevPage + 1));
  }

  private ClickableItem getPageItem(Player player, int page, ItemStack icon) {
    return ClickableItem.of(icon, c -> getInventory().open(player, page));
  }

  private final ItemStack getPageIcon(String text, int page) {
    return getNamedItem(text, Material.ARROW, page);
  }

  public ItemStack getNamedItem(String text, Material material, int amount) {
    ItemStack stack = new ItemStack(material, amount);
    ItemMeta meta = stack.getItemMeta();
    meta.setDisplayName(colorize(text));
    meta.addItemFlags(ItemFlag.values());
    stack.setItemMeta(meta);
    return stack;
  }
}

package dev.pgm.community.party.menu.hosts;

import static tc.oc.pgm.util.bukkit.BukkitUtils.colorize;

import com.google.common.collect.Lists;
import dev.pgm.community.party.feature.MapPartyFeature;
import dev.pgm.community.party.hosts.MapPartyHosts;
import dev.pgm.community.party.menu.MapPartyMenu;
import dev.pgm.community.utils.SkullUtils;
import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.Pagination;
import fr.minuskube.inv.content.SlotIterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.util.inventory.ItemBuilder;
import tc.oc.pgm.util.skin.Skin;

public class HostMenu extends MapPartyMenu {

  private static final String TITLE = "&a&lEvent Hosts";
  private static final int ROWS = 6;
  private static final boolean HOST_ONLY = false;

  public HostMenu(MapPartyFeature feature, Player viewer) {
    super(feature, TITLE, ROWS, HOST_ONLY, viewer);
    open();
  }

  public void render(Player player, InventoryContents contents) {
    contents.fillBorders(getBorderItem());

    if (getFeature().getParty() != null && getFeature().getParty().getHosts() != null) {
      MapPartyHosts hosts = getFeature().getParty().getHosts();

      // Main host
      contents.set(1, 4, getMainHostIcon(hosts));

      Set<ClickableItem> players =
          hosts.getSubHostIds().stream()
              .map(id -> getSubHostIcon(hosts, id))
              .collect(Collectors.toSet());

      Pagination page = contents.pagination();
      page.setItems(players.toArray(new ClickableItem[players.size()]));
      page.setItemsPerPage(7);

      page.addToIterator(contents.newIterator(SlotIterator.Type.HORIZONTAL, 2, 1));

      // No results
      if (players.isEmpty()) {
        contents.set(2, 4, getNoPlayersIcon());
      }

      // Previous
      if (!page.isFirst()) {
        contents.set(3, 1, getPrevPageItem(player, page.getPage() - 1));
      }

      // Next
      if (!page.isLast()) {
        contents.set(3, 7, getNextPageItem(player, page.getPage() + 1));
      }

      // Add new host button
      contents.set(
          4,
          4,
          ClickableItem.of(
              SkullUtils.customSkull(ADD_SKIN, "&a&lAdd Host", "&7Click to add event host"),
              c -> {
                new HostAddMenu(hosts).open(getViewer());
              }));

      // Return to party menu
      contents.set(5, 4, getMainMenuIcon());
    }
  }

  @Override
  public void init(Player player, InventoryContents contents) {
    render(player, contents);
  }

  @Override
  public void update(Player player, InventoryContents contents) {
    render(player, contents);
  }

  private ClickableItem getSubHostIcon(MapPartyHosts hosts, UUID playerId) {
    return ClickableItem.of(
        getHostHead(hosts, playerId),
        c -> {
          close();
          String name = hosts.getCachedName(playerId);
          Bukkit.dispatchCommand(getViewer(), "event hosts remove " + name);
        });
  }

  private ClickableItem getMainHostIcon(MapPartyHosts hosts) {
    return ClickableItem.empty(getHostHead(hosts, hosts.getMainHostId()));
  }

  private ClickableItem getNoPlayersIcon() {
    return ClickableItem.empty(
        new ItemBuilder()
            .material(Material.STAINED_GLASS_PANE)
            .color(DyeColor.RED)
            .name(colorize("&cNo hosts found"))
            .lore(colorize("&7Add hosts using the button below"))
            .flags(ItemFlag.values())
            .build());
  }

  private ItemStack getHostHead(MapPartyHosts hosts, UUID playerId) {
    Player player = Bukkit.getPlayer(playerId);
    boolean online = player != null;
    boolean mainHost = hosts.isMainHost(playerId);

    String displayName = online ? player.getDisplayName() : "&3" + hosts.getCachedName(playerId);
    Skin skin = hosts.getCachedSkin(playerId);

    List<String> lore = Lists.newArrayList();

    if (mainHost) {
      lore.add(colorize("&e&lMain Event Host"));
    } else {
      lore.add(colorize("&cClick to remove host"));
    }

    return getPlayerHead(hosts.getCachedName(playerId), displayName, lore, playerId, skin);
  }
}

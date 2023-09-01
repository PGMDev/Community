package dev.pgm.community.party.menu.hosts;

import static tc.oc.pgm.util.bukkit.BukkitUtils.colorize;

import com.google.common.collect.Lists;
import dev.pgm.community.Community;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.menu.PlayerSelectionProvider;
import dev.pgm.community.party.hosts.MapPartyHosts;
import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import java.util.List;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public class HostAddMenu extends PlayerSelectionProvider {

  private final MapPartyHosts hosts;
  private final SmartInventory inventory;

  public HostAddMenu(MapPartyHosts hosts) {
    this.hosts = hosts;
    this.inventory =
        SmartInventory.builder()
            .size(6, 9)
            .manager(Community.get().getInventory())
            .provider(this)
            .title(colorize("&a&lAdd New Host&7:"))
            .build();
  }

  @Override
  public void init(Player player, InventoryContents contents) {
    super.init(player, contents);
    contents.set(
        5,
        4,
        ClickableItem.of(
            getNamedItem("&7Return to &aEvent Hosts", Material.CAKE, 1),
            c -> {
              Bukkit.dispatchCommand(player, "event hosts");
            }));
  }

  @Override
  public SmartInventory getInventory() {
    return inventory;
  }

  @Override
  public Consumer<InventoryClickEvent> getClickEvent(Player target) {
    return c -> {
      Player viewer = (Player) c.getWhoClicked();
      if (viewer.hasPermission(CommunityPermissions.PARTY) && !hosts.isHost(target.getUniqueId())) {
        viewer.closeInventory();
        Bukkit.dispatchCommand(viewer, "event hosts add " + target.getName());
      }
    };
  }

  @Override
  public List<String> getPlayerLore(Player viewer, Player player) {
    boolean isHost = hosts.isHost(player.getUniqueId());
    return Lists.newArrayList(
        colorize(
            isHost
                ? "&cAlready a host"
                : "&aClick to make " + player.getDisplayName() + " &aa host"));
  }
}

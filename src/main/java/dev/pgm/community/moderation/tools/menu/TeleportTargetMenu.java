package dev.pgm.community.moderation.tools.menu;

import static tc.oc.pgm.util.bukkit.BukkitUtils.colorize;

import com.google.common.collect.Lists;
import dev.pgm.community.Community;
import dev.pgm.community.menu.PlayerSelectionProvider;
import dev.pgm.community.moderation.tools.TeleportToolManager;
import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import java.util.List;
import java.util.function.Consumer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public class TeleportTargetMenu extends PlayerSelectionProvider {

  private final SmartInventory inventory;
  private final TeleportToolManager tools;

  public TeleportTargetMenu(TeleportToolManager tools) {
    this.tools = tools;
    this.inventory =
        SmartInventory.builder()
            .size(6, 9)
            .manager(Community.get().getInventory())
            .provider(this)
            .title(colorize("&eSelect Target&7:"))
            .build();
  }

  @Override
  public void init(Player player, InventoryContents contents) {
    super.init(player, contents);
    if (tools.hasTarget(player)) {
      contents.set(
          5,
          4,
          ClickableItem.of(
              getNamedItem("&cReset selection", Material.BARRIER, 1),
              c -> {
                c.setCancelled(true);
                tools.unTarget(player);
                player.closeInventory();
              }));
    }
  }

  @Override
  public Consumer<InventoryClickEvent> getClickEvent(Player target) {
    return c -> {
      Player viewer = (Player) c.getWhoClicked();
      tools.targetPlayer(viewer, target);
      c.setCancelled(true);
      viewer.closeInventory();
    };
  }

  @Override
  public List<String> getPlayerLore(Player viewer, Player target) {
    boolean isTarget = tools.isTarget(viewer, target);
    String select =
        "&6\u00BB " + (isTarget ? "&aCurrent target" : "&eClick to select") + " &6\u00AB";
    return Lists.newArrayList(colorize(select));
  }

  @Override
  public SmartInventory getInventory() {
    return inventory;
  }
}

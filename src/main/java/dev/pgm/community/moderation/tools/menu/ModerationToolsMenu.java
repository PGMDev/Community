package dev.pgm.community.moderation.tools.menu;

import static tc.oc.pgm.util.bukkit.BukkitUtils.colorize;

import dev.pgm.community.Community;
import dev.pgm.community.freeze.FreezeFeature;
import dev.pgm.community.moderation.tools.TeleportToolManager;
import dev.pgm.community.moderation.tools.obs.FlightSpeedTool;
import dev.pgm.community.moderation.tools.obs.GamemodeTool;
import dev.pgm.community.moderation.tools.obs.NightVisionTool;
import dev.pgm.community.moderation.tools.obs.ObsVisibilityTool;
import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.listeners.AntiGriefListener;
import tc.oc.pgm.spawns.ObserverToolFactory;
import tc.oc.pgm.util.inventory.ItemBuilder;

public class ModerationToolsMenu implements InventoryProvider {

  private SmartInventory inventory;

  public ModerationToolsMenu() {
    this.inventory =
        SmartInventory.builder()
            .manager(Community.get().getInventory())
            .size(4, 9)
            .title(colorize("&aModeration Tools"))
            .provider(this)
            .build();
  }

  public void open(Player viewer) {
    inventory.open(viewer);
  }

  @Override
  public void init(Player player, InventoryContents contents) {
    contents.set(0, 1, new FlightSpeedTool(player).getItem());
    contents.set(0, 3, new NightVisionTool(player).getItem());
    contents.set(0, 5, new ObsVisibilityTool(player).getItem());
    contents.set(0, 7, new GamemodeTool(player).getItem());

    contents.fillRow(
        1,
        ClickableItem.empty(
            new ItemBuilder()
                .material(Material.STAINED_GLASS_PANE)
                .color(DyeColor.BLACK)
                .name(colorize("&r "))
                .flags(ItemFlag.values())
                .build()));

    ObserverToolFactory pgmObsTools = new ObserverToolFactory(PGM.get());
    contents.set(
        2,
        0,
        ClickableItem.of(
            TeleportToolManager.TP_HOOK, c -> addItem(player, TeleportToolManager.TP_HOOK)));
    contents.set(
        2,
        2,
        ClickableItem.of(
            FreezeFeature.getFreezeTool(player),
            c -> addItem(player, FreezeFeature.getFreezeTool(player))));
    contents.set(
        2,
        4,
        ClickableItem.of(
            AntiGriefListener.getDefuseItem(player),
            c -> addItem(player, AntiGriefListener.getDefuseItem(player))));
    contents.set(
        2,
        6,
        ClickableItem.of(
            pgmObsTools.getEditWand(player),
            c -> addItem(player, pgmObsTools.getEditWand(player))));
    contents.set(
        2,
        8,
        ClickableItem.of(
            pgmObsTools.getTeleportTool(player),
            c -> addItem(player, pgmObsTools.getTeleportTool(player))));
  }

  private void addItem(Player player, ItemStack item) {
    player.getInventory().addItem(item);
  }

  @Override
  public void update(Player player, InventoryContents contents) {}
}

package dev.pgm.community.moderation.tools;

import static tc.oc.pgm.util.bukkit.BukkitUtils.colorize;

import dev.pgm.community.moderation.tools.menu.ModerationToolsMenu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.player.event.ObserverInteractEvent;
import tc.oc.pgm.util.inventory.ItemBuilder;

public class ModerationTools {

  private final Material MOD_TOOLS_MATERIAL = Material.EMERALD;

  private final ItemStack TOOL_ITEM =
      new ItemBuilder()
          .material(MOD_TOOLS_MATERIAL)
          .name(colorize("&a&lModerator Tools"))
          .lore(colorize("&7Click to open the mod tools menu."))
          .flags(ItemFlag.values())
          .build();

  private ModerationToolsMenu menu;
  private TeleportToolManager tpHook;

  public ModerationTools() {
    this.menu = new ModerationToolsMenu();
    this.tpHook = new TeleportToolManager();
  }

  public TeleportToolManager getTeleportHook() {
    return tpHook;
  }

  public void giveTool(Player player) {
    player.getInventory().setItem(8, TOOL_ITEM);
  }

  public void onInteract(ObserverInteractEvent event) {
    if (event.getClickedItem() != null) {
      ItemStack tool = event.getClickedItem();
      if (tool.isSimilar(TOOL_ITEM)) {
        openMenu(event.getPlayer().getBukkit());
        event.setCancelled(true);
      }
    }
  }

  public void openMenu(Player player) {
    menu.open(player);
  }
}

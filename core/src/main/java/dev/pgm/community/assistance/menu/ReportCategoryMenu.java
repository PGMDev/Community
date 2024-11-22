package dev.pgm.community.assistance.menu;

import static tc.oc.pgm.util.bukkit.BukkitUtils.colorize;

import dev.pgm.community.assistance.feature.AssistanceFeature;
import dev.pgm.community.menu.CommunityInventoryProvider;
import fr.minuskube.inv.InventoryManager;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import java.util.List;
import java.util.function.Consumer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.util.bukkit.BukkitUtils;
import tc.oc.pgm.util.inventory.ItemBuilder;

public class ReportCategoryMenu extends CommunityInventoryProvider<ReportCategory> {

  private InventoryManager manager;
  private AssistanceFeature assistance;
  private Player target;

  public ReportCategoryMenu(
      InventoryManager manager,
      Player target,
      AssistanceFeature assistance,
      List<ReportCategory> categories) {
    super(categories);
    this.manager = manager;
    this.assistance = assistance;
    this.target = target;
  }

  private void openReasonMenu(Player player, Player target, ReportCategory category) {
    SmartInventory.builder()
        .manager(manager)
        .title(colorize("&eSelect a reason&7:"))
        .size(1, 9)
        .provider(new ReportReasonsMenu(assistance, target, category))
        .build()
        .open(player);
  }

  @Override
  public Consumer<InventoryClickEvent> getClickAction(
      ReportCategory category, Player viewer, InventoryContents contents) {
    return e -> {
      openReasonMenu(viewer, target, category);
    };
  }

  @Override
  protected ItemStack getNoResultsItem() {
    return new ItemBuilder()
        .material(Material.BARRIER)
        .name(BukkitUtils.colorize("&c&lNo Categories found"))
        .lore(BukkitUtils.colorize("&7There has been an error! &cPlease alert a staff member"))
        .flags(ItemFlag.values())
        .build();
  }
}

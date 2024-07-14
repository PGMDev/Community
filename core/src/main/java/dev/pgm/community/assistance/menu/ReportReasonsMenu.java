package dev.pgm.community.assistance.menu;

import dev.pgm.community.assistance.feature.AssistanceFeature;
import dev.pgm.community.menu.CommunityInventoryProvider;
import fr.minuskube.inv.content.InventoryContents;
import java.util.function.Consumer;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.util.bukkit.BukkitUtils;
import tc.oc.pgm.util.inventory.ItemBuilder;

public class ReportReasonsMenu extends CommunityInventoryProvider<ReportReason> {

  private AssistanceFeature feature;
  private ReportCategory category;
  private Player target;

  public ReportReasonsMenu(AssistanceFeature feature, Player target, ReportCategory category) {
    super(category.getReasons());
    this.feature = feature;
    this.target = target;
    this.category = category;
  }

  @Override
  public Consumer<InventoryClickEvent> getClickAction(
      ReportReason reason, Player viewer, InventoryContents contents) {
    return e -> {
      feature.report(
          viewer,
          target,
          ChatColor.stripColor(String.format("%s - %s", category.getName(), reason.getName())));
      viewer.closeInventory();
    };
  }

  @Override
  protected ItemStack getNoResultsItem() {
    return new ItemBuilder()
        .material(Material.BARRIER)
        .name(BukkitUtils.colorize("&c&lNo Reasons found"))
        .lore(BukkitUtils.colorize("&7No reasons provided in config for " + category.getName()))
        .flags(ItemFlag.values())
        .build();
  }
}

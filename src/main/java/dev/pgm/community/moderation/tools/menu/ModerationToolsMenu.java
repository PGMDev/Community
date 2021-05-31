package dev.pgm.community.moderation.tools.menu;

import static tc.oc.pgm.util.bukkit.BukkitUtils.colorize;

import dev.pgm.community.Community;
import dev.pgm.community.moderation.tools.buttons.types.GamemodeButton;
import dev.pgm.community.moderation.tools.buttons.types.NightVisionButton;
import dev.pgm.community.moderation.tools.buttons.types.ObserverVisibilityButton;
import dev.pgm.community.moderation.tools.buttons.types.SpeedButton;
import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import java.util.Random;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.util.bukkit.BukkitUtils;
import tc.oc.pgm.util.inventory.ItemBuilder;

public class ModerationToolsMenu implements InventoryProvider {

  private SmartInventory inventory;
  private Random random;

  public ModerationToolsMenu() {
    this.inventory =
        SmartInventory.builder()
            .manager(Community.get().getInventory())
            .size(4, 9)
            .title(colorize("&aModeration Tools"))
            .provider(this)
            .build();
    this.random = new Random();
  }

  public void open(Player viewer) {
    inventory.open(viewer);
  }

  @Override
  public void init(Player player, InventoryContents contents) {
    contents.set(0, 1, new SpeedButton(player).getItem());
    contents.set(0, 3, new NightVisionButton(player).getItem());
    contents.set(0, 5, new ObserverVisibilityButton(player).getItem());
    contents.set(0, 7, new GamemodeButton(player).getItem());

    contents.fillRow(
        1,
        ClickableItem.empty(
            new ItemBuilder()
                .material(Material.STAINED_GLASS_PANE)
                .color(DyeColor.BLACK)
                .name(colorize("&4 Coming Soon"))
                .flags(ItemFlag.values())
                .build()));
  }

  private void addItem(Player player, ItemStack item) {
    player.getInventory().addItem(item);
  }

  @Override
  public void update(Player player, InventoryContents contents) {
    int state = contents.property("state", 0);
    contents.setProperty("state", state + 1);

    if (state % 10 != 0) return;

    short color = (short) random.nextInt(15);
    DyeColor dye = DyeColor.values()[color];
    ChatColor cColor = BukkitUtils.dyeColorToChatColor(dye);

    contents.fillRow(
        1,
        ClickableItem.empty(
            new ItemBuilder()
                .material(Material.STAINED_GLASS_PANE)
                .color(DyeColor.values()[color])
                .name(colorize(cColor + "&lMore tools coming soon!"))
                .flags(ItemFlag.values())
                .build()));
  }
}

package dev.pgm.community.party.menu.modifiers;

import static tc.oc.pgm.util.bukkit.BukkitUtils.colorize;

import dev.pgm.community.Community;
import dev.pgm.community.party.feature.MapPartyFeature;
import dev.pgm.community.party.menu.MapPartyMenu;
import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.content.InventoryContents;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import tc.oc.pgm.util.inventory.ItemBuilder;

public class MapPartyModifierMenu extends MapPartyMenu {

  private static final String TITLE = "&5&lModifiers";
  private static final int ROWS = 6;
  private static final boolean HOST_ONLY = true;

  public MapPartyModifierMenu(MapPartyFeature feature, Player viewer) {
    super(feature, TITLE, ROWS, HOST_ONLY, viewer);
    open();
  }

  private void render(Player player, InventoryContents contents) {
    contents.fillBorders(getBorderItem());

    if (getFeature().getParty() == null) {
      return;
    }
    contents.set(2, 3, getMutationIcon());
    contents.set(2, 5, getRaindropMultiplierIcon());
    contents.set(5, 4, getMainMenuIcon());
  }

  @Override
  public void init(Player player, InventoryContents contents) {
    render(player, contents);
  }

  @Override
  public void update(Player player, InventoryContents contents) {
    render(player, contents);
  }

  private ClickableItem getRaindropMultiplierIcon() {
    return ClickableItem.of(
        new ItemBuilder()
            .material(Material.GHAST_TEAR)
            .amount(2)
            .name(colorize("&b&lRaindrop Multiplier"))
            .lore(
                colorize("&7Click to toggle multiplier"),
                "",
                colorize(
                    "&7Status&8: "
                        + (getFeature().isRaindropMultiplierActive() ? "&aEnabled" : "&cDisabled")))
            .flags(ItemFlag.values())
            .build(),
        c -> {
          close();
          Bukkit.dispatchCommand(getViewer(), "event raindrops");
        });
  }

  private ClickableItem getMutationIcon() {
    if (Community.get().getFeatures().getMutations().isEnabled()) {
      return ClickableItem.of(
          new ItemBuilder()
              .material(Material.ENCHANTED_BOOK)
              .name(colorize("&a&lMutations"))
              .lore(colorize("&7Click to view match mutations"))
              .flags(ItemFlag.values())
              .build(),
          c -> {
            Bukkit.dispatchCommand(getViewer(), "mutation");
          });
    }

    return ClickableItem.empty(
        new ItemBuilder()
            .material(Material.BARRIER)
            .name(colorize("&cDisabled"))
            .lore(colorize("&7Mutations are disabled! You can enable then in the config"))
            .flags(ItemFlag.values())
            .build());
  }
}

package dev.pgm.community.mutations.menu;

import static tc.oc.pgm.util.bukkit.BukkitUtils.colorize;

import dev.pgm.community.mutations.Mutation;
import dev.pgm.community.mutations.feature.MutationFeature;
import dev.pgm.community.mutations.options.MutationBooleanOption;
import dev.pgm.community.mutations.options.MutationListOption;
import dev.pgm.community.mutations.options.MutationOption;
import dev.pgm.community.mutations.options.MutationRangeOption;
import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import java.util.List;
import java.util.stream.Collectors;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import tc.oc.pgm.util.inventory.ItemBuilder;

public class MutationOptionsMenu implements InventoryProvider {

  private final MutationFeature mutations;

  public MutationOptionsMenu(MutationFeature mutations) {
    this.mutations = mutations;
  }

  @Override
  public void init(Player player, InventoryContents contents) {
    render(player, contents);
  }

  @Override
  public void update(Player player, InventoryContents contents) {
    render(player, contents);
  }

  private void render(Player player, InventoryContents contents) {
    contents.fill(null);

    List<MutationOption> options =
        mutations.getMutations().stream()
            .map(Mutation::getOptions)
            .flatMap(mo -> mo.stream())
            .collect(Collectors.toList());

    if (options.isEmpty()) {
      contents.set(1, 4, getNoMutationsIcon());
    }

    for (MutationOption option : options) {
      contents.add(getOptionIcon(option));
    }

    contents.set(3, 4, getReturnIcon(player));
  }

  private ClickableItem getNoMutationsIcon() {
    return ClickableItem.empty(
        new ItemBuilder()
            .material(Material.SIGN)
            .name(colorize("&c&lNo mutation options found!"))
            .lore(colorize("&7Options only display when mutation is enabled"))
            .flags(ItemFlag.values())
            .build());
  }

  private ClickableItem getReturnIcon(Player viewer) {
    return ClickableItem.of(
        new ItemBuilder()
            .material(Material.BARRIER)
            .name(colorize("&eReturn to Mutations"))
            .flags(ItemFlag.values())
            .build(),
        c -> {
          mutations.getMenu().open(viewer);
        });
  }

  private ClickableItem getOptionIcon(MutationOption option) {
    if (option instanceof MutationBooleanOption) {
      return getToggleIcon((MutationBooleanOption) option);
    }

    if (option instanceof MutationRangeOption) {
      return getRangeIcon((MutationRangeOption) option);
    }

    if (option instanceof MutationListOption) {
      return getListIcon((MutationListOption) option);
    }

    return null; // Option type not implemented yet
  }

  private ClickableItem getToggleIcon(MutationBooleanOption option) {
    return ClickableItem.of(
        getItem(option)
            .lore(
                ChatColor.DARK_AQUA + option.getDescription(),
                ChatColor.GRAY
                    + "Default"
                    + ChatColor.WHITE
                    + ": "
                    + getBooleanValue(option.getDefaultValue()),
                ChatColor.GRAY
                    + "Value"
                    + ChatColor.WHITE
                    + ": "
                    + getBooleanValue(option.getValue()))
            .build(),
        c -> {
          option.setValue(!option.getValue());
        });
  }

  private ClickableItem getRangeIcon(MutationRangeOption option) {
    return ClickableItem.of(
        getItem(option)
            .lore(
                ChatColor.DARK_AQUA + option.getDescription(),
                ChatColor.GRAY
                    + "Default"
                    + ChatColor.WHITE
                    + ": "
                    + ChatColor.GOLD
                    + option.getDefaultValue(),
                ChatColor.GRAY
                    + "Current"
                    + ChatColor.WHITE
                    + ": "
                    + ChatColor.YELLOW
                    + option.getValue(),
                option.isPrerequisite()
                    ? ChatColor.GRAY
                        + "("
                        + ChatColor.DARK_PURPLE
                        + "Applies when mutation is enabled"
                        + ChatColor.GRAY
                        + ")"
                    : "")
            .build(),
        c -> {
          int value = option.getValue();
          boolean increase = c.isLeftClick();

          if (increase && value >= option.getMax()) {
            value = option.getMin();
          } else if (!increase && value <= option.getMin()) {
            value = option.getMax();
          } else {
            value = value + (increase ? 1 : -1);
          }
          option.setValue(value);
        });
  }

  private ClickableItem getListIcon(MutationListOption option) {
    return ClickableItem.of(
        getItem(option)
            .lore(
                ChatColor.DARK_AQUA + option.getDescription(),
                ChatColor.GRAY
                    + "Default"
                    + ChatColor.WHITE
                    + ": "
                    + ChatColor.GOLD
                    + option.getDefaultValue(),
                ChatColor.GRAY
                    + "Current"
                    + ChatColor.WHITE
                    + ": "
                    + ChatColor.YELLOW
                    + option.getValue(),
                option.isPrerequisite()
                    ? ChatColor.GRAY
                        + "("
                        + ChatColor.DARK_PURPLE
                        + "Applies when mutation is enabled"
                        + ChatColor.GRAY
                        + ")"
                    : "")
            .build(),
        c -> option.toggle(c.isLeftClick()));
  }

  private ItemBuilder getItem(MutationOption option) {
    return new ItemBuilder()
        .material(option.getIconMaterial())
        .name(ChatColor.GREEN + option.getName())
        .flags(ItemFlag.values());
  }

  private String getBooleanValue(boolean value) {
    return value ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled";
  }
}

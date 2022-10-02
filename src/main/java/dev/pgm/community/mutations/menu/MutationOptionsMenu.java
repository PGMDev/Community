package dev.pgm.community.mutations.menu;

import dev.pgm.community.mutations.options.MutationBooleanOption;
import dev.pgm.community.mutations.options.MutationOption;
import dev.pgm.community.mutations.options.MutationRangeOption;
import dev.pgm.community.mutations.types.gameplay.BlitzMutation;
import dev.pgm.community.mutations.types.items.ExplosionMutation;
import dev.pgm.community.mutations.types.mechanics.DoubleJumpMutation;
import dev.pgm.community.mutations.types.mechanics.FlyMutation;
import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import tc.oc.pgm.util.inventory.ItemBuilder;

public class MutationOptionsMenu implements InventoryProvider {

  @Override
  public void init(Player player, InventoryContents contents) {
    render(contents);
  }

  @Override
  public void update(Player player, InventoryContents contents) {
    render(contents);
  }

  private void render(InventoryContents contents) {
    contents.fill(null);
    contents.add(getOptionIcon(BlitzMutation.BLITZ_LIVES));
    contents.add(getOptionIcon(DoubleJumpMutation.JUMP_POWER));
    contents.add(getOptionIcon(FlyMutation.FLY_DISABLE_DELAY));
    contents.add(getOptionIcon(FlyMutation.FLY_SPEED));
    contents.add(getOptionIcon(ExplosionMutation.FIREBALL_COOLDOWN));
    contents.add(getOptionIcon(ExplosionMutation.FIREBALL_POWER));
    contents.add(getOptionIcon(ExplosionMutation.FIREBALL_FIRE));
  }

  private ClickableItem getOptionIcon(MutationOption option) {
    if (option instanceof MutationBooleanOption) {
      return getToggleIcon((MutationBooleanOption) option);
    }

    if (option instanceof MutationRangeOption) {
      return getRangeIcon((MutationRangeOption) option);
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
                    + ChatColor.GOLD
                    + option.getDefaultValue(),
                ChatColor.GRAY
                    + "Value"
                    + ChatColor.WHITE
                    + ": "
                    + (option.getValue()
                        ? ChatColor.GREEN + "Enabled"
                        : ChatColor.RED + "Disabled"))
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

  private ItemBuilder getItem(MutationOption option) {
    return new ItemBuilder()
        .material(option.getIconMaterial())
        .name(ChatColor.GREEN + option.getName())
        .flags(ItemFlag.values());
  }
}

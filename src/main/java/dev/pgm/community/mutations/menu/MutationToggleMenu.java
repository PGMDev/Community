package dev.pgm.community.mutations.menu;

import dev.pgm.community.mutations.Mutation;
import dev.pgm.community.mutations.MutationType;
import dev.pgm.community.mutations.feature.MutationFeature;
import dev.pgm.community.utils.CommandAudience;
import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import java.util.Optional;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import tc.oc.pgm.util.inventory.ItemBuilder;

public class MutationToggleMenu implements InventoryProvider {

  private MutationFeature mutations;

  public MutationToggleMenu(MutationFeature mutations) {
    this.mutations = mutations;
  }

  @Override
  public void init(Player player, InventoryContents contents) {
    CommandAudience viewer = new CommandAudience(player);
    for (MutationType type : MutationType.values()) {
      Optional<Mutation> mutation = mutations.getMutation(type);
      contents.add(
          ClickableItem.of(
              type.getIcon(mutation.isPresent()),
              e -> {
                if (!mutations.hasMutation(type)) {
                  mutations.addMutation(viewer, type, true);
                } else {
                  mutations.removeMutation(viewer, type);
                }
                e.setCancelled(true);
                e.setCurrentItem(type.getIcon(mutations.getMutation(type).isPresent()));
              }));
    }

    contents.set(3, 4, getOptionIcon(player));
  }

  private ClickableItem getOptionIcon(Player player) {
    return ClickableItem.of(
        new ItemBuilder()
            .material(Material.WORKBENCH)
            .name(ChatColor.GOLD + "Options")
            .lore(ChatColor.GRAY + "Click to view active mutation options")
            .flags(ItemFlag.values())
            .build(),
        c -> {
          mutations.getOptionMenu().open(player);
        });
  }

  @Override
  public void update(Player player, InventoryContents contents) {}
}

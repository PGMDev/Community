package dev.pgm.community.mutations.menu;

import dev.pgm.community.mutations.Mutation;
import dev.pgm.community.mutations.MutationType;
import dev.pgm.community.mutations.feature.MutationFeature;
import dev.pgm.community.utils.CommandAudience;
import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import java.util.Optional;
import org.bukkit.entity.Player;

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
  }

  @Override
  public void update(Player player, InventoryContents contents) {}
}

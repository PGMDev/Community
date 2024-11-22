package dev.pgm.community.mutations.types.items;

import com.google.common.collect.Sets;
import dev.pgm.community.mutations.Mutation;
import dev.pgm.community.mutations.MutationBase;
import dev.pgm.community.mutations.MutationType;
import dev.pgm.community.mutations.options.MutationBooleanOption;
import dev.pgm.community.mutations.options.MutationOption;
import java.util.Collection;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.kits.ClearItemsKit;
import tc.oc.pgm.spawns.events.ParticipantKitApplyEvent;

public class NoSpawnKitMutation extends MutationBase {

  private static final MutationBooleanOption CLEAR_ITEMS_OPTION =
      new MutationBooleanOption(
          "Clear Items", "Whether items should be removed", Material.STICK, true, false);

  private static final MutationBooleanOption CLEAR_ARMOR_OPTION =
      new MutationBooleanOption(
          "Clear Armor", "Whether armor should be removed", Material.CHAINMAIL_HELMET, true, false);

  private static final MutationBooleanOption CLEAR_EFFECTS_OPTION =
      new MutationBooleanOption(
          "Clear Potion Effects",
          "Whether potion effects should be removed",
          Material.GLASS_BOTTLE,
          false,
          false);

  public NoSpawnKitMutation(Match match) {
    super(match, MutationType.NO_SPAWN_KIT);
  }

  @Override
  public Collection<MutationOption> getOptions() {
    return Sets.newHashSet(CLEAR_ITEMS_OPTION, CLEAR_ARMOR_OPTION, CLEAR_EFFECTS_OPTION);
  }

  @Override
  public boolean canEnable(Set<Mutation> existingMutations) {
    return true;
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onSpawn(ParticipantKitApplyEvent event) {
    event
        .getPlayer()
        .applyKit(
            new ClearItemsKit(
                CLEAR_ITEMS_OPTION.getValue(),
                CLEAR_ARMOR_OPTION.getValue(),
                CLEAR_EFFECTS_OPTION.getValue()),
            true);
  }
}

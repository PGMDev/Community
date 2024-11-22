package dev.pgm.community.mutations.types.mechanics;

import com.google.common.collect.Sets;
import dev.pgm.community.mutations.Mutation;
import dev.pgm.community.mutations.MutationType;
import dev.pgm.community.mutations.types.KitMutationBase;
import java.util.Set;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.kits.PotionKit;

public class BlindMutation extends KitMutationBase {

  public BlindMutation(Match match) {
    super(match, MutationType.BLIND, getBlindKit());
  }

  @Override
  public boolean canEnable(Set<Mutation> existing) {
    return true;
  }

  private static PotionKit getBlindKit() {
    return new PotionKit(
        Sets.newHashSet(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 1)));
  }
}

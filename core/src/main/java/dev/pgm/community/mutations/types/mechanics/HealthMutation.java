package dev.pgm.community.mutations.types.mechanics;

import dev.pgm.community.mutations.Mutation;
import dev.pgm.community.mutations.MutationType;
import dev.pgm.community.mutations.types.KitMutationBase;
import java.util.Set;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.kits.MaxHealthKit;

public class HealthMutation extends KitMutationBase {

  public HealthMutation(Match match) {
    super(match, MutationType.HEALTH, getHealthKit());
  }

  @Override
  public boolean canEnable(Set<Mutation> existing) {
    return true;
  }

  @Override
  public void spawn(MatchPlayer player) {
    super.spawn(player);
    player.getBukkit().addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 3, 9));
  }

  private static MaxHealthKit getHealthKit() {
    return new MaxHealthKit(40);
  }
}

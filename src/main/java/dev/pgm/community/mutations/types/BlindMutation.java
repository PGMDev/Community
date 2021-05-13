package dev.pgm.community.mutations.types;

import com.google.common.collect.Sets;
import dev.pgm.community.mutations.MutationType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.kits.PotionKit;

public class BlindMutation extends KitMutationBase {

  public BlindMutation(Match match) {
    super(match, MutationType.BLIND, getBlindKit());
  }

  @Override
  public void disable() {
    super.disable();
    removeMatchBlindness();
  }

  @Override
  public boolean canEnable() {
    return true;
  }

  private static PotionKit getBlindKit() {
    return new PotionKit(
        Sets.newHashSet(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 1)));
  }

  private void removeMatchBlindness() {
    match.getParticipants().stream().forEach(this::removeBlindKit);
  }

  private void removeBlindKit(MatchPlayer player) {
    getBlindKit().remove(player);
  }
}

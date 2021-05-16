package dev.pgm.community.mutations.types;

import dev.pgm.community.mutations.MutationType;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.kits.MaxHealthKit;

public class HealthMutation extends KitMutationBase {

  public HealthMutation(Match match) {
    super(match, MutationType.HEALTH, getHealthKit());
  }

  @Override
  public void disable() {
    super.disable();
    removeMatchMaxHealth();
  }

  @Override
  public boolean canEnable() {
    return true;
  }

  @Override
  public void spawn(MatchPlayer player) {
    super.spawn(player);
    player
        .getBukkit()
        .setHealth(player.getBukkit().getMaxHealth()); // Reset health to max upon respawn
  }

  private static MaxHealthKit getHealthKit() {
    return new MaxHealthKit(40);
  }

  private void removeMatchMaxHealth() {
    match.getParticipants().stream().forEach(this::removeMaxHealthKit);
  }

  private void removeMaxHealthKit(MatchPlayer player) {
    getHealthKit().remove(player);
  }
}

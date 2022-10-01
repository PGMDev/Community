package dev.pgm.community.mutations.types.mechanics;

import dev.pgm.community.mutations.Mutation;
import dev.pgm.community.mutations.MutationBase;
import dev.pgm.community.mutations.MutationType;
import java.util.Set;
import tc.oc.pgm.api.match.Match;

public class FriendlyFireMutation extends MutationBase {

  public FriendlyFireMutation(Match match) {
    super(match, MutationType.FRIENDLY);
  }

  @Override
  public boolean canEnable(Set<Mutation> existing) {
    return !match.isFriendlyFire();
  }

  @Override
  public void enable() {
    super.enable();
    match.setFriendlyFire(true);
  }

  @Override
  public void disable() {
    match.setFriendlyFire(false);
    super.disable();
  }
}

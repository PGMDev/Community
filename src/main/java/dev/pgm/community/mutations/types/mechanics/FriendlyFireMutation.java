package dev.pgm.community.mutations.types.mechanics;

import dev.pgm.community.mutations.Mutation;
import dev.pgm.community.mutations.MutationBase;
import dev.pgm.community.mutations.MutationType;
import java.util.Set;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.scoreboard.ScoreboardMatchModule;

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
    forceTeamUpdate();
  }

  @Override
  public void disable() {
    match.setFriendlyFire(false);
    forceTeamUpdate();
    super.disable();
  }

  private void forceTeamUpdate() {
    ScoreboardMatchModule smm = match.getModule(ScoreboardMatchModule.class);
    if (smm == null) return;
    smm.updateAll();
  }
}

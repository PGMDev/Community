package dev.pgm.community.mutations.types.arrows;

import com.google.common.collect.Sets;
import dev.pgm.community.mutations.Mutation;
import dev.pgm.community.mutations.MutationBase;
import dev.pgm.community.mutations.MutationType;
import dev.pgm.community.mutations.types.BowMutation;
import java.util.Set;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.filters.matcher.StaticFilter;
import tc.oc.pgm.modules.ModifyBowProjectileMatchModule;

public class TNTBowMutation extends MutationBase implements BowMutation {

  private ModifyBowProjectileMatchModule module;

  public TNTBowMutation(Match match) {
    super(match, MutationType.TNT_BOW);
  }

  @Override
  public boolean canEnable(Set<Mutation> existing) {
    return match.getModule(ModifyBowProjectileMatchModule.class) == null
        && !hasBowMutation(existing);
  }

  @Override
  public void enable() {
    super.enable();
    this.module =
        new ModifyBowProjectileMatchModule(
            match, TNTPrimed.class, 1, Sets.newHashSet(), StaticFilter.ALLOW);
    module.enable();
    match.addListener(module, MatchScope.RUNNING);
  }

  @Override
  public void disable() {
    super.disable();
    if (module != null) {
      module.disable();
      HandlerList.unregisterAll(module);
    }
  }
}

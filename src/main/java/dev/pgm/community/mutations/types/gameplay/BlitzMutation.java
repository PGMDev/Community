package dev.pgm.community.mutations.types.gameplay;

import com.google.common.collect.Sets;
import dev.pgm.community.mutations.Mutation;
import dev.pgm.community.mutations.MutationBase;
import dev.pgm.community.mutations.MutationType;
import dev.pgm.community.mutations.options.MutationOption;
import dev.pgm.community.mutations.options.MutationRangeOption;
import java.util.Collection;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.blitz.BlitzConfig;
import tc.oc.pgm.blitz.BlitzMatchModule;
import tc.oc.pgm.filters.matcher.StaticFilter;

/** BlitzMutation - Enables blitz on a non-blitz match * */
public class BlitzMutation extends MutationBase {

  private static MutationRangeOption BLITZ_LIVES =
      new MutationRangeOption(
          "Blitz Lives", "The number of lives per-user", Material.EGG, true, 1, 1, 999);

  private BlitzMatchModule blitz;

  public BlitzMutation(Match match) {
    super(match, MutationType.BLITZ);
  }

  @Override
  public Collection<MutationOption> getOptions() {
    return Sets.newHashSet(BLITZ_LIVES);
  }

  @Override
  public void enable() {
    super.enable();
    this.blitz =
        new BlitzMatchModule(
            match,
            new BlitzConfig(
                BLITZ_LIVES.getValue(),
                true,
                StaticFilter.ALLOW,
                StaticFilter.ALLOW,
                StaticFilter.DENY));

    blitz.enable();
    match.addListener(blitz, MatchScope.RUNNING);
  }

  @Override
  public void disable() {
    super.disable();
    if (blitz != null) {
      blitz.disable();
      HandlerList.unregisterAll(blitz);
    }
  }

  @Override
  public boolean canEnable(Set<Mutation> existing) {
    return match.getModule(BlitzMatchModule.class) == null;
  }
}

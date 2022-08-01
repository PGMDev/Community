package dev.pgm.community.mutations.types;

import dev.pgm.community.mutations.MutationBase;
import dev.pgm.community.mutations.MutationType;
import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.blitz.BlitzConfig;
import tc.oc.pgm.blitz.BlitzMatchModule;
import tc.oc.pgm.filters.matcher.StaticFilter;

/** BlitzMutation - Enables blitz (1 life) on a non-blitz match * */
public class BlitzMutation extends MutationBase {

  private BlitzMatchModule blitz;

  public BlitzMutation(Match match) {
    super(match, MutationType.BLITZ);
  }

  @Override
  public void enable() {
    super.enable();
    this.blitz =
        new BlitzMatchModule(
            match,
            new BlitzConfig(1, true, StaticFilter.ALLOW)); // TODO: allow command for more lives

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
  public boolean canEnable() {
    return match.getModule(BlitzMatchModule.class) == null;
  }
}

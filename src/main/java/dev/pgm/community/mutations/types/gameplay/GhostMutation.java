package dev.pgm.community.mutations.types.gameplay;

import com.google.common.collect.Sets;
import dev.pgm.community.mutations.Mutation;
import dev.pgm.community.mutations.MutationType;
import dev.pgm.community.mutations.types.KitMutationBase;
import java.util.Set;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.NameTagVisibility;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.kits.PotionKit;

public class GhostMutation extends KitMutationBase {

  private static final PotionEffect EFFECT =
      new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 1, false, false);
  private static final PotionKit KIT = new PotionKit(Sets.newHashSet(EFFECT));

  public GhostMutation(Match match) {
    super(match, MutationType.GHOST, KIT);
  }

  @Override
  public void enable() {
    super.enable();
    match.getParties().stream()
        .filter(party -> party instanceof Competitor)
        .forEach(
            party ->
                ((Competitor) party)
                    .setNameTagVisibilityOverride(NameTagVisibility.HIDE_FOR_OTHER_TEAMS));
    ;
  }

  @Override
  public void disable() {
    super.disable();
    match.getParties().stream()
        .filter(party -> party instanceof Competitor)
        .forEach(
            party -> ((Competitor) party).setNameTagVisibilityOverride(NameTagVisibility.ALWAYS));
  }

  @Override
  public boolean canEnable(Set<Mutation> existing) {
    return true;
  }
}

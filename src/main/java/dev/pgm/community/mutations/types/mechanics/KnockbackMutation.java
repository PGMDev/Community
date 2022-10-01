package dev.pgm.community.mutations.types.mechanics;

import dev.pgm.community.mutations.Mutation;
import dev.pgm.community.mutations.MutationBase;
import dev.pgm.community.mutations.MutationType;
import java.util.Set;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerAttackEntityEvent;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;

public class KnockbackMutation extends MutationBase {

  public KnockbackMutation(Match match) {
    super(match, MutationType.KNOCKBACK);
  }

  @Override
  public boolean canEnable(Set<Mutation> existing) {
    return true;
  }

  @EventHandler
  public void onPlayerDamage(PlayerAttackEntityEvent event) {
    Entity target = event.getLeftClicked();
    MatchPlayer player = match.getParticipant(event.getPlayer());
    if (target == null || player == null || target.isDead()) return;
    Vector playerLocation = player.getLocation().getDirection();
    target.setVelocity(playerLocation.setY(1).multiply(2 + match.getRandom().nextInt(4)));
  }
}

package dev.pgm.community.mutations.types.mechanics;

import dev.pgm.community.mutations.Mutation;
import dev.pgm.community.mutations.MutationType;
import dev.pgm.community.mutations.options.MutationRangeOption;
import dev.pgm.community.mutations.types.KitMutationBase;
import java.util.Set;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.doublejump.DoubleJumpKit;

/** DoubleJumpMutation - Enables {@link DoubleJumpKit} and no fall damage for all players * */
public class DoubleJumpMutation extends KitMutationBase {

  public static MutationRangeOption JUMP_POWER =
      new MutationRangeOption(
          "Jump Power", "Power of double jump", MutationType.JUMP.getMaterial(), true, 2, 1, 10);

  public DoubleJumpMutation(Match match) {
    super(match, MutationType.JUMP, getJumpKit(true));
  }

  @Override
  public void disable() {
    super.disable();
    giveAllKit(getJumpKit(false));
  }

  @Override
  public boolean canEnable(Set<Mutation> existing) {
    return true;
  }

  @EventHandler
  public void onFallDamage(EntityDamageEvent event) {
    if (event.getCause() != DamageCause.FALL) return;
    if (event.getEntityType() != EntityType.PLAYER) return;
    event.setCancelled(true);
  }

  private static DoubleJumpKit getJumpKit(
      boolean enabled) { // TODO: add option for random or defined values
    return new DoubleJumpKit(enabled, JUMP_POWER.getValue(), DoubleJumpKit.DEFAULT_RECHARGE, false);
  }
}

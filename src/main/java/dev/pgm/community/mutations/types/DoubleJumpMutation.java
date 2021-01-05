package dev.pgm.community.mutations.types;

import dev.pgm.community.mutations.MutationType;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.doublejump.DoubleJumpKit;

public class DoubleJumpMutation extends KitMutation {

  public DoubleJumpMutation(Match match) {
    super(match, MutationType.JUMP, getJumpKit(true));
  }

  @Override
  public void disable() {
    super.disable();
    giveAllKit(getJumpKit(false));
  }

  @Override
  public boolean canEnable() {
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
    return new DoubleJumpKit(
        enabled, DoubleJumpKit.DEFAULT_POWER, DoubleJumpKit.DEFAULT_RECHARGE, false);
  }
}

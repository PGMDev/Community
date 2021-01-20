package dev.pgm.community.mutations.types;

import dev.pgm.community.mutations.MutationBase;
import dev.pgm.community.mutations.MutationType;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import tc.oc.pgm.api.match.Match;

/** BlockExplosionMutation - Random explosions when mining blocks * */
public class BlockExplosionMutation extends MutationBase {

  private static final double EXPLODE_CHANCE = 0.05;

  public BlockExplosionMutation(Match match) {
    super(match, MutationType.EXPLOSIONS);
  }

  @Override
  public boolean canEnable() {
    return true;
  }

  @EventHandler
  public void onBlockBreak(BlockBreakEvent event) {
    Player player = event.getPlayer();
    if (player != null && match.getParticipant(player) != null) {
      if (Math.random() < EXPLODE_CHANCE) {
        explode(event.getBlock());
      }
    }
  }

  private void explode(Block block) {
    Location loc = block.getLocation();
    loc.getWorld().createExplosion(loc, 3.3f);
    for (int i = 0; i < 5; i++)
      loc.getWorld().spigot().playEffect(loc, Effect.LAVA_POP, 0, 0, 0, 0, 0, 0, 15, 50);
  }
}

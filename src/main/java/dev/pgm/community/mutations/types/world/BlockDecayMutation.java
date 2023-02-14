package dev.pgm.community.mutations.types.world;

import com.google.common.collect.Maps;
import dev.pgm.community.mutations.Mutation;
import dev.pgm.community.mutations.MutationType;
import dev.pgm.community.mutations.options.MutationRangeOption;
import dev.pgm.community.mutations.types.ScheduledMutationBase;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;

public class BlockDecayMutation extends ScheduledMutationBase {

  public static MutationRangeOption DECAY_SECONDS =
      new MutationRangeOption(
          "Decay Delay",
          "Delay of time before blocks decay",
          MutationType.BLOCK_DECAY.getMaterial(),
          false,
          5,
          1,
          60);

  private Map<Location, Long> placedBlocks = Maps.newHashMap();

  public BlockDecayMutation(Match match) {
    super(match, MutationType.BLOCK_DECAY, 1);
  }

  @Override
  public boolean canEnable(Set<Mutation> existing) {
    return true;
  }

  @Override
  public void disable() {
    if (this.placedBlocks != null && !this.placedBlocks.isEmpty()) {
      this.placedBlocks.keySet().forEach(location -> location.getBlock().setType(Material.AIR));
      this.placedBlocks.clear();
    }
    super.disable();
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onBlockPlace(BlockPlaceEvent event) {
    Block block = event.getBlock();
    MatchPlayer player = match.getParticipant(event.getPlayer());
    if (block == null || block.getType() == Material.AIR) return;
    if (player == null) return;
    this.placedBlocks.put(block.getLocation(), getDelayedTime(DECAY_SECONDS.getValue()));
  }

  private long getDelayedTime(int delay) {
    return (System.currentTimeMillis() / 1000) + delay;
  }

  @Override
  public void run() {
    Iterator<Entry<Location, Long>> e = placedBlocks.entrySet().iterator();
    while (e.hasNext()) {
      Entry<Location, Long> block = e.next();
      if (block.getValue() <= getDelayedTime(0)) {
        block.getKey().getBlock().setType(Material.AIR);
        e.remove();
      }
    }
  }
}

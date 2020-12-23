package dev.pgm.community.mutations.types;

import com.google.common.collect.Lists;
import dev.pgm.community.Community;
import dev.pgm.community.mutations.MutationType;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;

public class TNTRainMutation extends ScheduledMutationBase {

  private static final int TASK_SECONDS = 30;
  private static final int MAX_PLAYERS_PER_RUN = 15;
  private static final String TNT_META = "mutation_tnt";

  private static final boolean TNT_KNOCKBACK = true;
  private static final double TNT_KNOCKBACK_STRENGTH = 1.35D;
  private static final double TNT_KNOCKBACK_ARC = 1.55D;

  public TNTRainMutation(Match match) {
    super(match, MutationType.TNTRAIN, TASK_SECONDS);
  }

  public void spawnTNT(Player player) {
    Location loc = player.getLocation().clone().add(0, 10, 0); // TODO: configure this!!!
    TNTPrimed tnt = loc.getWorld().spawn(loc, TNTPrimed.class);
    tnt.setIsIncendiary(random.nextBoolean()); // MAKE RANDOM
    tnt.setMetadata(TNT_META, new FixedMetadataValue(Community.get(), true));
  }

  @Override
  public boolean canEnable() {
    return true;
  }

  @Override
  public void run() {
    List<MatchPlayer> players = new ArrayList<MatchPlayer>(match.getParticipants());
    if (players.isEmpty()) return;

    List<MatchPlayer> spawned = Lists.newArrayList();
    while (spawned.size() < Math.min(MAX_PLAYERS_PER_RUN, players.size())) {
      MatchPlayer randomPl = players.get(random.nextInt(players.size()));
      if (randomPl != null && !spawned.contains(randomPl)) {
        spawnTNT(randomPl.getBukkit());
        spawned.add(randomPl);
      }
    }
  }

  @EventHandler
  public void onTNTExplode(EntityExplodeEvent event) {
    if (event.getEntity() instanceof TNTPrimed) {
      TNTPrimed tnt = (TNTPrimed) event.getEntity();
      if (tnt.hasMetadata(TNT_META)) {
        event.setYield(random.nextFloat() + 1f);

        if (TNT_KNOCKBACK) {
          tnt.getLocation()
              .getWorld()
              .getNearbyEntities(tnt.getLocation(), 5, 5, 5)
              .forEach(ent -> fakeKnockback(ent, tnt.getLocation()));
        }

        tnt.getWorld()
            .spigot()
            .playEffect(tnt.getLocation(), Effect.LAVA_POP, 0, 0, 0, 0, 0, 1, 10, 50);
      }
    }
  }

  private void fakeKnockback(Entity ent, Location loc) {
    fakeKnockback(ent, loc, TNT_KNOCKBACK_STRENGTH, TNT_KNOCKBACK_ARC);
  }

  private void fakeKnockback(Entity ent, Location loc, double multiply, double arc) {
    if (ent instanceof Creature || ent instanceof Player) {
      double dX = loc.getX() - ent.getLocation().getX();
      double dY = loc.getY() - ent.getLocation().getY();
      double dZ = loc.getZ() - ent.getLocation().getZ();
      double yaw = Math.atan2(dZ, dX);
      double pitch = Math.atan2(Math.sqrt(dZ * dZ + dX * dX), dY) + Math.PI;
      double X = Math.sin(pitch) * Math.cos(yaw);
      double Y = Math.sin(pitch) * Math.sin(yaw);
      double Z = Math.cos(pitch);

      Vector vector = new Vector(X, Z, Y);
      ent.setVelocity(vector.multiply(multiply).add(new Vector(0, arc, 0)));
    }
  }
}

package dev.pgm.community.mutations.types.mechanics;

import com.google.common.collect.Lists;
import dev.pgm.community.Community;
import dev.pgm.community.mutations.Mutation;
import dev.pgm.community.mutations.MutationType;
import dev.pgm.community.mutations.options.MutationListOption;
import dev.pgm.community.mutations.types.ScheduledMutationBase;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Skeleton.SkeletonType;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.ffa.FreeForAllMatchModule;
import tc.oc.pgm.points.PointProviderAttributes;
import tc.oc.pgm.points.RandomPointProvider;
import tc.oc.pgm.points.RegionPointProvider;
import tc.oc.pgm.regions.CuboidRegion;
import tc.oc.pgm.teams.Team;

public class MobMutation extends ScheduledMutationBase {

  private static final int UPDATE_DELAY = 60;
  private static final int RANDOM_DISTANCE = 45;
  private static final String MOB_METADATA = "mob-mutation";

  public static MutationListOption<Integer> TOTAL_MOBS =
      new MutationListOption(
          "Total Mobs",
          "Total number of mobs spawned",
          MutationType.MOBS.getMaterial(),
          false,
          Lists.newArrayList(25, 50, 100, 125, 200, 250, 300));

  public MobMutation(Match match) {
    super(match, MutationType.MOBS, UPDATE_DELAY);
  }

  @Override
  public boolean canEnable(Set<Mutation> existingMutations) {
    return match.getModule(FreeForAllMatchModule.class) == null;
  }

  @Override
  public void disable() {
    super.disable();
    for (LivingEntity mob : this.getSpawnedMobs()) {
      mob.remove();
    }
  }

  @Nullable
  private RandomPointProvider getPointProvider() {
    Vector loc1 = null;
    Vector loc2 = null;

    for (Party party : match.getParties()) {
      if (!(party instanceof Team)) continue;
      if (party.getPlayers().isEmpty()) continue;
      MatchPlayer player = party.getPlayers().stream().findAny().orElse(null);
      if (player == null) continue;
      if (loc1 == null) {
        loc1 = player.getLocation().toVector();
      } else {
        loc2 = player.getLocation().toVector();
        break;
      }
    }

    // Not enough players, so fetch a random location near loc1
    if (loc1 != null && loc2 == null) {
      loc2 = loc1.clone().add(Vector.getRandom().multiply(RANDOM_DISTANCE));
    }

    if (loc1 != null && loc2 != null) {
      return new RandomPointProvider(
          Collections.singleton(
              new RegionPointProvider(
                  new CuboidRegion(loc1, loc2), new PointProviderAttributes())));
    }

    return null;
  }

  private EntityType[] MOB_TYPES = {
    EntityType.ZOMBIE,
    EntityType.SKELETON,
    EntityType.SPIDER,
    EntityType.ENDERMAN,
    EntityType.PIG_ZOMBIE,
    EntityType.CREEPER,
    EntityType.WITCH
  };

  public void spawnMob(Location loc, EntityType type) {
    Entity mob = loc.getWorld().spawnEntity(loc, type);
    mob.setVelocity(Vector.getRandom());
    mob.setMetadata(MOB_METADATA, new FixedMetadataValue(Community.get(), true));

    // Special case for spider jockey
    if (type == EntityType.SPIDER && match.getRandom().nextFloat() < 0.25) {
      Skeleton jockey = (Skeleton) loc.getWorld().spawnEntity(loc, EntityType.SKELETON);
      jockey.setSkeletonType(
          match.getRandom().nextBoolean() ? SkeletonType.WITHER : SkeletonType.NORMAL);
      applyMetadata(jockey);
      mob.setPassenger(jockey);
    }
  }

  private void applyMetadata(Entity entity) {
    entity.setMetadata(MOB_METADATA, new FixedMetadataValue(Community.get(), true));
  }

  public EntityType getRandomType() {
    return MOB_TYPES[random.nextInt(MOB_TYPES.length)];
  }

  private int getTotalSpawned() {
    return getSpawnedMobs().size();
  }

  private List<LivingEntity> getSpawnedMobs() {
    return match.getWorld().getLivingEntities().stream()
        .filter(le -> le.hasMetadata(MOB_METADATA))
        .collect(Collectors.toList());
  }

  @Override
  public void run() {
    RandomPointProvider provider = getPointProvider();
    if (provider == null) return;
    for (int total = getTotalSpawned(); total < TOTAL_MOBS.getValue(); total++) {
      Location loc = provider.getPoint(match, null);
      if (loc != null) {
        loc.getWorld().spigot().playEffect(loc, Effect.FLAME, 0, 0, 0, 0, 0, 0, 5, 100);
        spawnMob(loc, getRandomType());
      }
    }
  }
}

package dev.pgm.community.mutations.types.mapdev;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import dev.pgm.community.mutations.Mutation;
import dev.pgm.community.mutations.MutationType;
import dev.pgm.community.mutations.options.MutationListOption;
import dev.pgm.community.mutations.options.MutationOption;
import dev.pgm.community.mutations.options.MutationRangeOption;
import dev.pgm.community.mutations.types.ScheduledMutationBase;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.core.Core;
import tc.oc.pgm.core.CoreMatchModule;
import tc.oc.pgm.destroyable.Destroyable;
import tc.oc.pgm.destroyable.DestroyableMatchModule;
import tc.oc.pgm.flag.FlagMatchModule;
import tc.oc.pgm.flag.Post;
import tc.oc.pgm.kits.PotionKit;
import tc.oc.pgm.points.PointProvider;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamFactory;
import tc.oc.pgm.teams.TeamMatchModule;
import tc.oc.pgm.util.Pair;
import tc.oc.pgm.wool.MonumentWool;
import tc.oc.pgm.wool.WoolMatchModule;

public class ProximityEffectsMutation extends ScheduledMutationBase {

  MutationListOption<Integer> attackerDistanceOption =
      new MutationListOption<>(
          "Attacker Distance",
          "Radius where potion effects are applied",
          Material.COMPASS,
          true,
          Lists.newArrayList(0, 5, 10, 15, 20, 30, 40, 50, 75, 100, 150, 200));
  MutationListOption<Integer> defenderDistanceOption =
      new MutationListOption<>(
          "Defender Distance",
          "Radius where potion effects are applied",
          Material.EMPTY_MAP,
          true,
          Lists.newArrayList(0, 5, 10, 15, 20, 30, 40, 50, 75, 100, 150, 200));

  Map<PotionEffectType, MutationRangeOption> attackerOptions;
  Map<PotionEffectType, MutationRangeOption> defenderOptions;

  public ProximityEffectsMutation(Match match) {
    super(match, MutationType.PROXIMITY_EFFECT, 2);
    attackerOptions = buildOptions("Attacker");
    defenderOptions = buildOptions("Defender");
  }

  @Override
  public Collection<MutationOption> getOptions() {
    List<MutationOption> options = new ArrayList<>();

    options.add(attackerDistanceOption);
    options.addAll(attackerOptions.values());
    options.add(defenderDistanceOption);
    options.addAll(defenderOptions.values());

    return options;
  }

  @Override
  public boolean canEnable(Set<Mutation> existingMutations) {
    return match.hasModule(TeamMatchModule.class);
  }

  static Map<PotionEffectType, MutationRangeOption> buildOptions(String prefix) {
    HashMap<PotionEffectType, MutationRangeOption> effectOptionMap = new HashMap<>();

    effectOptionMap.put(
        PotionEffectType.FAST_DIGGING,
        new MutationRangeOption(
            prefix + " Haste", prefix + " Haste Amount", Material.DIAMOND_PICKAXE, true, 0, 0, 5));
    effectOptionMap.put(
        PotionEffectType.SPEED,
        new MutationRangeOption(
            prefix + " Speed", prefix + " Speed Amount", Material.SUGAR, true, 0, 0, 5));
    effectOptionMap.put(
        PotionEffectType.INCREASE_DAMAGE,
        new MutationRangeOption(
            prefix + " Strength",
            prefix + " Strength Amount",
            Material.DIAMOND_SWORD,
            true,
            0,
            0,
            5));
    effectOptionMap.put(
        PotionEffectType.JUMP,
        new MutationRangeOption(
            prefix + " Jump Boost",
            prefix + " Jump Boost Amount",
            Material.RABBIT_FOOT,
            true,
            0,
            0,
            5));
    effectOptionMap.put(
        PotionEffectType.REGENERATION,
        new MutationRangeOption(
            prefix + " Regeneration",
            prefix + " Regeneration Amount",
            Material.GHAST_TEAR,
            true,
            0,
            0,
            5));
    effectOptionMap.put(
        PotionEffectType.SLOW_DIGGING,
        new MutationRangeOption(
            prefix + " Mining Fatigue",
            prefix + " Mining Fatigue Amount",
            Material.WOOD_PICKAXE,
            true,
            0,
            0,
            5));
    effectOptionMap.put(
        PotionEffectType.SLOW,
        new MutationRangeOption(
            prefix + " Slowness", prefix + " Slowness Amount", Material.WEB, true, 0, 0, 5));
    effectOptionMap.put(
        PotionEffectType.WEAKNESS,
        new MutationRangeOption(
            prefix + " Weakness", prefix + " Weakness Amount", Material.WOOD_SWORD, true, 0, 0, 5));

    return effectOptionMap;
  }

  @Override
  public void run() {
    List<Pair<Vector, Team>> ownedLocationsMap = new ArrayList<>();
    List<Pair<Vector, Team>> attackingLocationsMap = new ArrayList<>();

    findADLocations(attackingLocationsMap, ownedLocationsMap);

    Set<MatchPlayer> attackers = new HashSet<>();
    Set<MatchPlayer> defenders = new HashSet<>();

    int defenderDistance = defenderDistanceOption.getValue();
    int attackerDistance = attackerDistanceOption.getValue();

    for (MatchPlayer player : match.getPlayers()) {
      fillPlayerSets(
          ownedLocationsMap, defenders, attackers, player, defenderDistance, attackerDistance);
      fillPlayerSets(
          attackingLocationsMap, attackers, defenders, player, attackerDistance, defenderDistance);
    }

    defenders.removeAll(attackers);

    applyPotionEffects(attackers, attackerOptions);
    applyPotionEffects(defenders, defenderOptions);
  }

  private void findADLocations(
      List<Pair<Vector, Team>> attackingLocationsMap, List<Pair<Vector, Team>> ownedLocationsMap) {
    TeamMatchModule teamMatchModule = match.moduleRequire(TeamMatchModule.class);
    Optional<WoolMatchModule> woolMatchModule = match.moduleOptional(WoolMatchModule.class);
    Optional<DestroyableMatchModule> destroyableMatchModule =
        match.moduleOptional(DestroyableMatchModule.class);
    Optional<CoreMatchModule> coreMatchModule = match.moduleOptional(CoreMatchModule.class);
    Optional<FlagMatchModule> flagMatchModule = match.moduleOptional(FlagMatchModule.class);
    //    Optional<ControlPointMatchModule> controlPointMatchModule =
    // match.moduleOptional(ControlPointMatchModule.class);
    // TODO: Control points not currently accessible

    if (woolMatchModule.isPresent()) {
      Multimap<Team, MonumentWool> wools = woolMatchModule.get().getWools();
      for (Map.Entry<Team, MonumentWool> entry : wools.entries()) {
        MonumentWool wool = entry.getValue();
        if (!wool.isCompleted()) {
          attackingLocationsMap.add(
              new Pair<>(entry.getValue().getDefinition().getLocation(), entry.getKey()));
        }
      }
    }

    if (destroyableMatchModule.isPresent()) {
      for (Destroyable destroyable : destroyableMatchModule.get().getDestroyables()) {
        if (!destroyable.isCompleted()) {
          ownedLocationsMap.add(
              new Pair<>(
                  destroyable.getBlockRegion().getBounds().getCenterPoint(),
                  destroyable.getOwner()));
        }
      }
    }

    if (coreMatchModule.isPresent()) {
      for (Core core : coreMatchModule.get().getCores()) {
        if (!core.isCompleted()) {
          ownedLocationsMap.add(
              new Pair<>(core.getCasingRegion().getBounds().getCenterPoint(), core.getOwner()));
        }
      }
    }

    if (flagMatchModule.isPresent()) {
      for (Post post : flagMatchModule.get().getPosts()) {
        TeamFactory ownerFactory = post.getOwner();
        if (ownerFactory != null) {
          Team owner = teamMatchModule.getTeam(ownerFactory);
          for (PointProvider returnPoint : post.getReturnPoints()) {
            ownedLocationsMap.add(
                new Pair<>(returnPoint.getRegion().getBounds().getCenterPoint(), owner));
          }
        }
      }
    }
  }

  private void applyPotionEffects(
      Set<MatchPlayer> players, Map<PotionEffectType, MutationRangeOption> options) {
    for (Map.Entry<PotionEffectType, MutationRangeOption> entry : options.entrySet()) {
      int level = entry.getValue().getValue();
      if (level > 0) {
        PotionKit potionKit =
            new PotionKit(
                Collections.singleton(new PotionEffect(entry.getKey(), 5 * 20, level - 1)));
        for (MatchPlayer defender : players) {
          defender.applyKit(potionKit, true);
        }
      }
    }
  }

  private static void fillPlayerSets(
      List<Pair<Vector, Team>> attackingLocationsMap,
      Set<MatchPlayer> attackers,
      Set<MatchPlayer> defenders,
      MatchPlayer player,
      int attackerDistance,
      int defenderDistance) {
    for (Pair<Vector, Team> teamVectorEntry : attackingLocationsMap) {
      if (teamVectorEntry.getRight().getPlayers().contains(player)) {
        if (player.getLocation().toVector().distance(teamVectorEntry.getLeft())
            < attackerDistance) {
          attackers.add(player);
        }
      } else {
        if (player.getLocation().toVector().distance(teamVectorEntry.getLeft())
            < defenderDistance) {
          defenders.add(player);
        }
      }
    }
  }
}

package dev.pgm.community.mutations.types.items;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dev.pgm.community.mutations.Mutation;
import dev.pgm.community.mutations.MutationType;
import dev.pgm.community.mutations.types.KitMutationBase;
import java.util.List;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.kits.ItemKit;
import tc.oc.pgm.kits.Kit;
import tc.oc.pgm.tnt.TNTMatchModule;

public class CannonSuppliesMutation extends KitMutationBase {
  public CannonSuppliesMutation(Match match) {
    super(match, MutationType.CANNON_SUPPLIES);
  }

  // Order is to allow partial kits to build functional cannons. The more items the player receives
  // the more complicated the cannons they can build
  static List<Kit> spawnKit =
      Lists.newArrayList(
          new ItemKit(
              Maps.newHashMap(),
              preventSharing(
                  Lists.newArrayList(
                      new ItemStack(Material.TNT, 64),
                      new ItemStack(Material.WATER_BUCKET),
                      new ItemStack(Material.STONE_BUTTON, 32),
                      new ItemStack(Material.LADDER, 32),
                      new ItemStack(Material.TNT, 64),
                      new ItemStack(Material.REDSTONE, 64),
                      new ItemStack(Material.WATER_BUCKET),
                      new ItemStack(Material.FENCE, 32),
                      new ItemStack(Material.WOOD_STEP, 64),
                      new ItemStack(Material.TNT, 64),
                      new ItemStack(Material.WOOD, 64),
                      new ItemStack(Material.TNT, 64),
                      new ItemStack(Material.DIODE, 64)))));

  static ItemKit killRewardKit =
      new ItemKit(
          Maps.newHashMap(), Lists.newArrayList(preventSharing(new ItemStack(Material.TNT, 16))));

  @Override
  public List<Kit> getKits() {
    return spawnKit;
  }

  @Override
  public boolean canEnable(Set<Mutation> existingMutations) {
    TNTMatchModule tntMatchModule = match.getModule(TNTMatchModule.class);
    if (tntMatchModule == null) return true;

    return !tntMatchModule.getProperties().instantIgnite;
  }

  @Override
  protected void givePlayerKit(MatchPlayer player, List<Kit> kits) {
    kits.forEach(kit -> player.applyKit(kit, false));
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onDeath(MatchPlayerDeathEvent event) {
    ParticipantState killer = event.getKiller();
    if (event.isChallengeKill() && killer != null) {
      MatchPlayer player = match.getPlayer(killer.getId());
      if (player != null && player.isAlive()) {
        player.applyKit(killRewardKit, false);
      }
    }
  }
}

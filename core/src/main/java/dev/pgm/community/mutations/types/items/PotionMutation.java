package dev.pgm.community.mutations.types.items;

import static dev.pgm.community.util.InventoryUtils.INVENTORY_UTILS;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dev.pgm.community.Community;
import dev.pgm.community.mutations.Mutation;
import dev.pgm.community.mutations.MutationType;
import dev.pgm.community.mutations.types.KitMutationBase;
import java.util.List;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.kits.ItemKit;
import tc.oc.pgm.kits.Kit;
import tc.oc.pgm.kits.tag.ItemTags;

/** PotionMutation - Random potions given on spawn and when mining blocks * */
public class PotionMutation extends KitMutationBase {

  private static final double SPLASH_CHANCE = 0.05;

  public PotionMutation(Match match) {
    super(match, MutationType.POTION);
  }

  @Override
  public List<Kit> getKits() {
    return Lists.newArrayList(getRandomPotionKit());
  }

  @EventHandler
  public void onBlockBreak(BlockBreakEvent event) {
    Player player = event.getPlayer();
    if (player != null && match.getParticipant(player) != null) {
      if (Math.random() < SPLASH_CHANCE) {
        splash(event.getBlock());
      }
    }
  }

  private void splash(Block block) {
    Location loc = block.getLocation();
    ThrownPotion splash = loc.getWorld().spawn(loc.clone().add(0.5, 0, 0.5), ThrownPotion.class);
    splash.setItem(getRandomPotionItem(true));
  }

  private static Kit getRandomPotionKit() {
    boolean randomSplash = Community.get().getRandom().nextBoolean();
    return new ItemKit(Maps.newHashMap(), Lists.newArrayList(getRandomPotionItem(randomSplash)));
  }

  public static ItemStack getRandomPotionItem(boolean splash) {
    ItemStack item = INVENTORY_UTILS.getRandomPotion(splash, Community.get().getRandom());
    ItemTags.PREVENT_SHARING.set(item, true);
    return item;
  }

  @Override
  public boolean canEnable(Set<Mutation> existing) {
    return true;
  }
}

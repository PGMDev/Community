package dev.pgm.community.mutations.types.kit;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dev.pgm.community.Community;
import dev.pgm.community.mutations.Mutation;
import dev.pgm.community.mutations.MutationType;
import dev.pgm.community.mutations.types.KitMutationBase;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionType;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.kits.ItemKit;
import tc.oc.pgm.kits.Kit;
import tc.oc.pgm.kits.tag.ItemTags;
import tc.oc.pgm.util.bukkit.BukkitUtils;

/** PotionMutation - Random potions given on spawn and when mining blocks * */
public class PotionMutation extends KitMutationBase {

  private static double SPLASH_CHANCE = 0.05;

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
    return getPotionItem(getRandomPotion(splash));
  }

  private static ItemStack getPotionItem(Potion potion) {
    ItemStack item = new ItemStack(Material.POTION);
    potion.apply(item);
    ItemMeta meta = item.getItemMeta();
    meta.setDisplayName(BukkitUtils.colorize("&d&lMystery Potion"));
    meta.addItemFlags(ItemFlag.values());
    item.setItemMeta(meta);
    ItemTags.PREVENT_SHARING.set(item, true);
    return item;
  }

  private static Potion getRandomPotion(boolean splash) {
    Random random = Community.get().getRandom();
    List<PotionType> safeTypes =
        Stream.of(PotionType.values())
            .filter(p -> p != PotionType.WATER) // No water lol
            .collect(Collectors.toList());
    PotionType randomType = safeTypes.get(random.nextInt(safeTypes.size()));
    return new Potion(randomType, 1, splash);
  }

  @Override
  public boolean canEnable(Set<Mutation> existing) {
    return true;
  }
}

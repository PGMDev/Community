package dev.pgm.community.mutations.types;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dev.pgm.community.Community;
import dev.pgm.community.mutations.MutationType;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.kits.ItemKit;
import tc.oc.pgm.kits.Kit;
import tc.oc.pgm.util.bukkit.BukkitUtils;
import tc.oc.pgm.util.inventory.tag.ItemTag;

/** ExplosionMutation - TNT, Fireballs, and random explosions when mining blocks * */
public class ExplosionMutation extends KitMutationBase {

  private static final double EXPLODE_CHANCE = 0.05;
  private static final int FIREBALL_COUNT = 5;

  private static final String EXPLOSION_METADATA = "mutation_explosion";
  private static final ItemTag<String> EXPLOSION_KIT = ItemTag.newString(EXPLOSION_METADATA);

  private Cache<UUID, String> lastFireball =
      CacheBuilder.newBuilder().expireAfterWrite(4, TimeUnit.SECONDS).build();

  public ExplosionMutation(Match match) {
    super(match, MutationType.EXPLOSION);
  }

  @Override
  public List<Kit> getKits() {
    return Lists.newArrayList(getRandomKit());
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onBlockBreak(BlockBreakEvent event) {
    if (isParticipant(event.getPlayer())) {
      if (Math.random() < EXPLODE_CHANCE) {
        explode(event.getBlock());
      }
    }
  }

  @EventHandler
  public void onFireballLaunch(PlayerInteractEvent event) {
    if (isParticipant(event.getPlayer())) {
      if (event.getItem() != null && event.getItem().getType() == Material.FIREBALL) {
        if (launchFireball(event.getPlayer())) {
          if (event.getItem().getAmount() == 1) {
            event.getPlayer().getInventory().remove(event.getItem());
          } else {
            event.getItem().setAmount(event.getItem().getAmount() - 1);
          }
          event.getPlayer().updateInventory();
        }
      }
    }
  }

  @EventHandler
  public void removeItemDrops(ItemSpawnEvent event) {
    if (event.getEntity() != null && event.getEntity().getItemStack() != null) {
      ItemStack stack = event.getEntity().getItemStack();
      String tag = EXPLOSION_KIT.get(stack);
      if (tag != null && tag.equalsIgnoreCase(EXPLOSION_METADATA)) {
        event.setCancelled(true);
      }
    }
  }

  private void explode(Block block) {
    Location loc = block.getLocation();
    loc.getWorld().createExplosion(loc, 3.3f);
    for (int i = 0; i < 5; i++)
      loc.getWorld().spigot().playEffect(loc, Effect.LAVA_POP, 0, 0, 0, 0, 0, 0, 15, 50);
  }

  private static Kit getRandomKit() {
    return Community.get().getRandom().nextBoolean() ? getTNTKit() : getFireballKit();
  }

  private static Kit getTNTKit() {
    List<ItemStack> items =
        Lists.newArrayList(
            new ItemStack(Material.TNT, 64),
            new ItemStack(Material.WOOD, 64),
            new ItemStack(Material.REDSTONE, 16),
            new ItemStack(Material.BUCKET));
    items = items.stream().map(ExplosionMutation::applyLore).collect(Collectors.toList());
    return new ItemKit(Maps.newHashMap(), items);
  }

  private static Kit getFireballKit() {
    return new ItemKit(Maps.newHashMap(), Lists.newArrayList(getFireballItem()));
  }

  private static ItemStack getFireballItem() {
    ItemStack stack = new ItemStack(Material.FIREBALL, FIREBALL_COUNT);
    ItemMeta meta = stack.getItemMeta();
    meta.setDisplayName(BukkitUtils.colorize("&4&lFireball"));
    meta.setLore(Lists.newArrayList(BukkitUtils.colorize("&7Click to launch fireball")));
    meta.addItemFlags(ItemFlag.values());
    stack.setItemMeta(meta);
    EXPLOSION_KIT.set(stack, EXPLOSION_METADATA);
    return stack;
  }

  private static ItemStack applyLore(ItemStack stack) {
    ItemMeta meta = stack.getItemMeta();
    meta.setLore(Lists.newArrayList(BukkitUtils.colorize("&cExplosion Mutation")));
    meta.addItemFlags(ItemFlag.values());
    stack.setItemMeta(meta);
    EXPLOSION_KIT.set(stack, EXPLOSION_METADATA);
    return stack;
  }

  private boolean launchFireball(Player player) {
    if (lastFireball.getIfPresent(player.getUniqueId()) == null) {
      Fireball fireball = player.launchProjectile(Fireball.class);
      fireball.setYield(Community.get().getRandom().nextInt(4) + 1);
      fireball.setIsIncendiary(true);
      lastFireball.put(player.getUniqueId(), "");
      return true;
    }

    return false;
  }

  @Override
  public boolean canEnable() {
    return true;
  }
}

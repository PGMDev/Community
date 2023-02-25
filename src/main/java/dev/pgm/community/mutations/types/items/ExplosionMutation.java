package dev.pgm.community.mutations.types.items;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dev.pgm.community.Community;
import dev.pgm.community.mutations.Mutation;
import dev.pgm.community.mutations.MutationType;
import dev.pgm.community.mutations.options.MutationBooleanOption;
import dev.pgm.community.mutations.options.MutationRangeOption;
import dev.pgm.community.mutations.types.KitMutationBase;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
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
import tc.oc.pgm.kits.tag.ItemTags;
import tc.oc.pgm.util.bukkit.BukkitUtils;
import tc.oc.pgm.util.inventory.tag.ItemTag;

/** ExplosionMutation - TNT, Fireballs, and random explosions when mining blocks * */
public class ExplosionMutation extends KitMutationBase {

  public static MutationRangeOption FIREBALL_POWER =
      new MutationRangeOption(
          "Fireball Power",
          "Power of fireball explosion",
          MutationType.EXPLOSION.getMaterial(),
          false,
          0,
          0,
          10);

  public static MutationBooleanOption FIREBALL_FIRE =
      new MutationBooleanOption(
          "Fireball Fire",
          "Whether fireballs are incendiary",
          MutationType.EXPLOSION.getMaterial(),
          true,
          false);

  public static MutationRangeOption FIREBALL_COOLDOWN =
      new MutationRangeOption(
          "Fireball Cooldown",
          "Delay between fireball shots",
          MutationType.EXPLOSION.getMaterial(),
          false,
          4,
          0,
          60);

  private static final double EXPLODE_CHANCE = 0.05;
  private static final int FIREBALL_COUNT = 5;

  private static final String EXPLOSION_METADATA = "mutation_explosion";
  private static final ItemTag<String> EXPLOSION_KIT = ItemTag.newString(EXPLOSION_METADATA);

  private Map<UUID, Long> lastFireball = Maps.newHashMap();

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
    World world = block.getWorld();

    if (loc == null || world == null) return;

    world.createExplosion(loc, 3.3f);

    for (int i = 0; i < 5; i++)
      world.spigot().playEffect(loc, Effect.LAVA_POP, 0, 0, 0, 0, 0, 0, 15, 50);
  }

  private static Kit getRandomKit() {
    return Community.get().getRandom().nextBoolean() ? getTNTKit() : getFireballKit();
  }

  private static Kit getTNTKit() {
    return new ItemKit(Maps.newHashMap(), getTNTItems());
  }

  private static Kit getFireballKit() {
    return new ItemKit(Maps.newHashMap(), Lists.newArrayList(getFireballItem()));
  }

  @Override
  public ItemStack[] getAllItems() {
    List<ItemStack> items = getTNTItems();
    items.add(getFireballItem());
    return items.toArray(new ItemStack[items.size()]);
  }

  private static List<ItemStack> getTNTItems() {
    List<ItemStack> items =
        Lists.newArrayList(
            new ItemStack(Material.TNT, 64),
            new ItemStack(Material.WOOD, 64),
            new ItemStack(Material.REDSTONE, 16),
            new ItemStack(Material.BUCKET));
    items = items.stream().map(ExplosionMutation::applyLore).collect(Collectors.toList());
    return items;
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
    ItemTags.PREVENT_SHARING.set(stack, true);
    return stack;
  }

  private boolean launchFireball(Player player) {
    Long last = lastFireball.get(player.getUniqueId());
    if (last != null) {
      long time = last / 1000;
      long now = System.currentTimeMillis() / 1000;
      if ((now - time) < FIREBALL_COOLDOWN.getValue()) {
        return false;
      }
    }

    int power = FIREBALL_POWER.getValue();
    Fireball fireball = player.launchProjectile(Fireball.class);
    fireball.setYield(power == 0 ? match.getRandom().nextInt(5) + 1 : power);
    fireball.setIsIncendiary(FIREBALL_FIRE.getValue());
    lastFireball.put(player.getUniqueId(), System.currentTimeMillis());
    return true;
  }

  @Override
  public boolean canEnable(Set<Mutation> existing) {
    return true;
  }
}

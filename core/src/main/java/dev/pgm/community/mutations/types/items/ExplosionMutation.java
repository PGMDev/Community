package dev.pgm.community.mutations.types.items;

import static tc.oc.pgm.util.bukkit.BukkitUtils.colorize;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import dev.pgm.community.Community;
import dev.pgm.community.mutations.Mutation;
import dev.pgm.community.mutations.MutationType;
import dev.pgm.community.mutations.options.MutationBooleanOption;
import dev.pgm.community.mutations.options.MutationOption;
import dev.pgm.community.mutations.options.MutationRangeOption;
import dev.pgm.community.mutations.types.KitMutationBase;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.kits.ItemKit;
import tc.oc.pgm.kits.Kit;
import tc.oc.pgm.kits.tag.ItemTags;
import tc.oc.pgm.util.inventory.ItemBuilder;
import tc.oc.pgm.util.inventory.tag.ItemTag;

/** ExplosionMutation - TNT, Fireballs, and random explosions when mining blocks * */
public class ExplosionMutation extends KitMutationBase {

  private static MutationRangeOption FIREBALL_POWER =
      new MutationRangeOption(
          "Fireball Power",
          "Power of fireball explosion",
          MutationType.EXPLOSION.getMaterial(),
          false,
          0,
          0,
          10);

  private static MutationBooleanOption FIREBALL_FIRE =
      new MutationBooleanOption(
          "Fireball Fire",
          "Whether fireballs are incendiary",
          MutationType.EXPLOSION.getMaterial(),
          true,
          false);

  private static MutationRangeOption LAUNCH_COOLDOWN =
      new MutationRangeOption(
          "Explosive Cooldown",
          "Delay between fireball or TNT shots",
          MutationType.EXPLOSION.getMaterial(),
          false,
          4,
          0,
          60);

  private static MutationBooleanOption MYSTERY_TNT =
      new MutationBooleanOption(
          "Mystery TNT",
          "Whether TNT should have a random effect",
          MutationType.EXPLOSION.getMaterial(),
          true,
          false);

  private static MutationRangeOption TNT_SIZE =
      new MutationRangeOption(
          "TNT Amount",
          "Amount of TNT given per player",
          MutationType.EXPLOSION.getMaterial(),
          false,
          6,
          1,
          64);

  private static final double EXPLODE_CHANCE = 0.05;
  private static final int FIREBALL_COUNT = 5;
  private static final Material TNT_LAUNCHER_MATERIAL = Material.NETHER_BRICK_ITEM;

  private static final String EXPLOSION_METADATA = "mutation_explosion";
  private static final ItemTag<String> EXPLOSION_KIT = ItemTag.newString(EXPLOSION_METADATA);

  private Map<UUID, Long> lastLaunch = Maps.newHashMap();

  public ExplosionMutation(Match match) {
    super(match, MutationType.EXPLOSION);
  }

  @Override
  public Collection<MutationOption> getOptions() {
    return Sets.newHashSet(FIREBALL_POWER, FIREBALL_FIRE, LAUNCH_COOLDOWN, MYSTERY_TNT, TNT_SIZE);
  }

  @Override
  public List<Kit> getKits() {
    return Lists.newArrayList(getRandomKit());
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onBlockPlace(BlockPlaceEvent event) {
    if (!isParticipant(event.getPlayer())) return;
    if (!MYSTERY_TNT.getValue()) return;

    Player player = event.getPlayer();
    ItemStack block = event.getItemInHand();
    if (block == null) return;
    if (!isFromKit(block)) return;
    if (block.getType() != Material.TNT) return;

    Location loc = event.getBlock().getLocation().add(0.5, 0, 0.5);

    int type = getRandom().nextInt(6);

    switch (type) {
      case 0:
        Creeper creeper = loc.getWorld().spawn(loc, Creeper.class);
        creeper.setVelocity(new Vector(0, 1, 0));
        break;
      case 1:
        TNTPrimed primed = loc.getWorld().spawn(loc, TNTPrimed.class);
        primed.setIsIncendiary(true);
        primed.setFuseTicks(40);
        primed.setYield(5f);
        primed.setVelocity(new Vector(0, 1, 0));
        Bukkit.getScheduler()
            .runTaskLater(
                Community.get(),
                () -> {
                  for (int i = 0; i < 4; i++) {
                    TNTPrimed additionalPrimed = loc.getWorld().spawn(loc, TNTPrimed.class);
                    additionalPrimed.setIsIncendiary(true);
                    additionalPrimed.setFuseTicks(40);
                    additionalPrimed.setYield(3f);

                    double randomAngle = Math.random() * 2 * Math.PI;
                    double x = Math.sin(randomAngle);
                    double z = Math.cos(randomAngle);
                    double y = Math.random() + 1;

                    additionalPrimed.setVelocity(new Vector(x, y, z));
                  }
                },
                20L);
        break;
      case 2:
        loc.getWorld().spigot().strikeLightning(loc, false);
        break;
      default:
        return; // If not a special type, we return here so we don't remove the block
    }

    event.getBlock().setType(Material.AIR);
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onBlockBreak(BlockBreakEvent event) {
    if (!isParticipant(event.getPlayer())) return;

    if (Math.random() < EXPLODE_CHANCE) {
      explode(event.getBlock());
    }
  }

  @EventHandler
  public void onExplosiveLaunch(PlayerInteractEvent event) {
    Player player = event.getPlayer();
    ItemStack item = event.getItem();

    if (!isParticipant(player)) return;
    if (item == null || item.getType() == Material.AIR) return;

    if (item.getType() != Material.FIREBALL && item.getType() != TNT_LAUNCHER_MATERIAL) return;
    if (item.getType() == TNT_LAUNCHER_MATERIAL && !isFromKit(item)) return;
    if (!launchExplosive(event.getPlayer(), item.getType())) return;

    if (event.getItem().getAmount() == 1) {
      event.getPlayer().getInventory().remove(event.getItem());
    } else {
      event.getItem().setAmount(event.getItem().getAmount() - 1);
    }
    event.getPlayer().updateInventory();
  }

  @EventHandler
  public void removeItemDrops(ItemSpawnEvent event) {
    if (event.getEntity() != null && event.getEntity().getItemStack() != null) {
      ItemStack stack = event.getEntity().getItemStack();
      if (isFromKit(stack)) {
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
    switch (Community.get().getRandom().nextInt(3)) {
      case 0:
        return getTNTKit();
      case 1:
        return getTNTLauncherKit();
      default:
        return getFireballKit();
    }
  }

  private static Kit getTNTKit() {
    return new ItemKit(Maps.newHashMap(), getTNTItems());
  }

  private static Kit getTNTLauncherKit() {
    return new ItemKit(Maps.newHashMap(), Lists.newArrayList(getTNTLauncherItem()));
  }

  private static Kit getFireballKit() {
    return new ItemKit(Maps.newHashMap(), Lists.newArrayList(getFireballItem()));
  }

  @Override
  public ItemStack[] getAllItems() {
    List<ItemStack> items = getTNTItems();
    items.add(getFireballItem());
    items.add(getTNTLauncherItem());
    return items.toArray(new ItemStack[items.size()]);
  }

  private static List<ItemStack> getTNTItems() {
    List<ItemStack> items =
        Lists.newArrayList(
            getTNTItem(), new ItemStack(Material.REDSTONE_TORCH_ON, TNT_SIZE.getValue()));
    items = items.stream().map(ExplosionMutation::applyLore).collect(Collectors.toList());
    return items;
  }

  private static ItemStack getTNTItem() {
    ItemBuilder builder =
        new ItemBuilder()
            .material(Material.TNT)
            .amount(TNT_SIZE.getValue())
            .name(colorize(MYSTERY_TNT.getValue() ? "&d&lMystery TNT" : "&c&lTNT"));
    if (MYSTERY_TNT.getValue()) {
      builder.lore(colorize("&7This TNT may have a special effect when placed!"));
    }
    return builder.build();
  }

  private static ItemStack getTNTLauncherItem() {
    ItemStack item =
        new ItemBuilder()
            .material(TNT_LAUNCHER_MATERIAL)
            .amount(TNT_SIZE.getValue())
            .name(colorize("&4&lTNT Launcher"))
            .lore("&7Click to launch TNT in the direction you are facing")
            .build();
    return applyLore(item);
  }

  private static ItemStack getFireballItem() {
    ItemStack item =
        new ItemBuilder()
            .material(Material.FIREBALL)
            .amount(FIREBALL_COUNT)
            .name(colorize("&4&lFireball"))
            .lore(colorize("&7Click to launch fireball"))
            .build();
    return applyLore(item);
  }

  private static ItemStack applyLore(ItemStack stack) {
    ItemMeta meta = stack.getItemMeta();
    List<String> lore = Lists.newArrayList();
    if (stack.getLore() != null) {
      lore = stack.getLore();
    }
    lore.add("");
    lore.add(colorize("&cExplosion Mutation"));
    meta.setLore(lore);
    meta.addItemFlags(ItemFlag.values());
    stack.setItemMeta(meta);
    EXPLOSION_KIT.set(stack, EXPLOSION_METADATA);
    ItemTags.PREVENT_SHARING.set(stack, true);
    return stack;
  }

  private boolean launchExplosive(Player player, Material type) {
    Long last = lastLaunch.get(player.getUniqueId());
    if (last != null) {
      long time = last / 1000;
      long now = System.currentTimeMillis() / 1000;
      if ((now - time) < LAUNCH_COOLDOWN.getValue()) {
        return false;
      }
    }

    if (type == Material.FIREBALL) {
      launchFireball(player);
    }

    if (type == TNT_LAUNCHER_MATERIAL) {
      launchTNT(player, 5);
    }

    lastLaunch.put(player.getUniqueId(), System.currentTimeMillis());
    return true;
  }

  public void launchFireball(Player player) {
    int power = FIREBALL_POWER.getValue();
    Fireball fireball = player.launchProjectile(Fireball.class);
    fireball.setYield(power == 0 ? match.getRandom().nextInt(5) + 1 : power);
    fireball.setIsIncendiary(FIREBALL_FIRE.getValue());
  }

  public void launchTNT(Player player, double length) {
    Location loc = player.getEyeLocation(); // Get the player's eye location
    Vector direction = loc.getDirection().normalize();

    TNTPrimed tnt = (TNTPrimed) player.getWorld().spawn(loc, TNTPrimed.class);
    tnt.setVelocity(direction.multiply(4));
    tnt.setFuseTicks(60);
  }

  private Random getRandom() {
    return match.getRandom();
  }

  private boolean isFromKit(ItemStack stack) {
    if (stack == null) return false;
    String tag = EXPLOSION_KIT.get(stack);
    return tag != null && tag.equals(EXPLOSION_METADATA);
  }

  @Override
  public boolean canEnable(Set<Mutation> existing) {
    return true;
  }
}

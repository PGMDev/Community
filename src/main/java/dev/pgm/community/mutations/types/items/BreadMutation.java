package dev.pgm.community.mutations.types.items;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dev.pgm.community.Community;
import dev.pgm.community.mutations.Mutation;
import dev.pgm.community.mutations.MutationType;
import dev.pgm.community.mutations.types.KitMutationBase;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.PluginManager;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.PlayerJoinMatchEvent;
import tc.oc.pgm.events.PlayerPartyChangeEvent;
import tc.oc.pgm.kits.ItemKit;
import tc.oc.pgm.kits.Kit;
import tc.oc.pgm.kits.tag.ItemTags;
import tc.oc.pgm.loot.WeightedRandomChooser;
import tc.oc.pgm.match.ObserverParty;
import tc.oc.pgm.util.block.BlockVectors;
import tc.oc.pgm.util.bukkit.WorldBorders;
import tc.oc.pgm.util.event.PlayerCoarseMoveEvent;
import tc.oc.pgm.util.inventory.ItemBuilder;

public class BreadMutation extends KitMutationBase {
  static final ImmutableMap<PotionEffectType, Double> BAD_POTION_MAP =
      new ImmutableMap.Builder<PotionEffectType, Double>()
          .put(PotionEffectType.WEAKNESS, 10.0)
          .put(PotionEffectType.SLOW, 10.0)
          .put(PotionEffectType.POISON, 10.0)
          .put(PotionEffectType.WITHER, 10.0)
          .put(PotionEffectType.BLINDNESS, 5.0)
          .put(PotionEffectType.HUNGER, 5.0)
          .put(PotionEffectType.CONFUSION, 5.0)
          .build();
  private static final ItemStack POTION_BREAD =
      preventSharing(
          new ItemBuilder(new ItemStack(Material.BREAD))
              .unbreakable(true)
              .name("Potion Bread")
              .build());

  private static final ItemStack ARMORED_BREAD =
      preventSharing(
          new ItemBuilder(new ItemStack(Material.BREAD))
              .unbreakable(true)
              .name("Armored Bread")
              .build());

  private static final ItemStack TELEPORT_BREAD =
      preventSharing(
          new ItemBuilder(new ItemStack(Material.BREAD))
              .unbreakable(true)
              .name("Teleport Bread")
              .build());
  static final ImmutableMap<ItemStack, Double> BREADS_MAP = getBreadsMap();
  static final ImmutableMap<ItemStack, Double> BAD_BREADS_MAP = getBadBreadsMap();
  private final WeightedRandomChooser<ItemStack> breadChooser;
  private final WeightedRandomChooser<ItemStack> badBreadChooser;
  private final Random random;
  private WeightedRandomChooser<PotionEffectType> potionChooser;
  private Set<UUID> badBreadSet;

  public BreadMutation(Match match) {
    super(match, MutationType.BREAD);

    random = Community.get().getRandom();
    breadChooser = new WeightedRandomChooser<>();
    breadChooser.addAll(BREADS_MAP);
    badBreadChooser = new WeightedRandomChooser<>();
    badBreadChooser.addAll(BAD_BREADS_MAP);
    potionChooser = new WeightedRandomChooser<>();
    potionChooser.addAll(BAD_POTION_MAP);
    badBreadSet = new HashSet<>();
  }

  static ItemStack preventSharing(ItemStack itemStack) {
    ItemTags.PREVENT_SHARING.set(itemStack, true);
    return itemStack;
  }

  static ImmutableMap<ItemStack, Double> getBreadsMap() {

    ItemStack ironBread =
        preventSharing(new ItemBuilder(new ItemStack(Material.BREAD)).name("Iron Bread").build());
    ItemMeta ironBreadMeta = ironBread.getItemMeta();
    ironBreadMeta.addAttributeModifier(
        Attribute.GENERIC_KNOCKBACK_RESISTANCE,
        new AttributeModifier(
            Attribute.GENERIC_KNOCKBACK_RESISTANCE.getName(),
            1,
            AttributeModifier.Operation.ADD_NUMBER));
    ironBread.setItemMeta(ironBreadMeta);

    ItemStack fastBread =
        preventSharing(new ItemBuilder(new ItemStack(Material.BREAD)).name("Fast Bread").build());
    ItemMeta speedBreadMeta = fastBread.getItemMeta();
    speedBreadMeta.addAttributeModifier(
        Attribute.GENERIC_MOVEMENT_SPEED,
        new AttributeModifier(
            Attribute.GENERIC_MOVEMENT_SPEED.getName(),
            0.3,
            AttributeModifier.Operation.ADD_SCALAR));
    fastBread.setItemMeta(speedBreadMeta);

    ItemStack veryFastBread =
        preventSharing(
            new ItemBuilder(new ItemStack(Material.BREAD)).name("Very Fast Bread").build());
    ItemMeta veryFastBreadMeta = veryFastBread.getItemMeta();
    veryFastBreadMeta.addAttributeModifier(
        Attribute.GENERIC_MOVEMENT_SPEED,
        new AttributeModifier(
            Attribute.GENERIC_MOVEMENT_SPEED.getName(), 1, AttributeModifier.Operation.ADD_SCALAR));
    veryFastBread.setItemMeta(veryFastBreadMeta);

    return new ImmutableMap.Builder<ItemStack, Double>()
        .put(
            preventSharing(
                new ItemBuilder(new ItemStack(Material.BREAD))
                    .enchant(Enchantment.FIRE_ASPECT, 1)
                    .name("Hot Bread")
                    .build()),
            20.0)
        .put(
            preventSharing(
                new ItemBuilder(new ItemStack(Material.BREAD))
                    .enchant(Enchantment.DAMAGE_ALL, 5)
                    .name("Sharp Bread")
                    .build()),
            20.0)
        .put(
            preventSharing(
                new ItemBuilder(new ItemStack(Material.BREAD))
                    .enchant(Enchantment.KNOCKBACK, 2)
                    .name("Bouncy Bread")
                    .build()),
            20.0)
        .put(ironBread, 10.0)
        .put(fastBread, 10.0)
        .put(POTION_BREAD, 10.0)
        .put(ARMORED_BREAD, 10.0)
        .put(TELEPORT_BREAD, 3.0)
        .put(veryFastBread, 3.0)
        .put(
            preventSharing(
                new ItemBuilder(new ItemStack(Material.BREAD))
                    .enchant(Enchantment.DAMAGE_ALL, 10)
                    .name("Very Sharp Bread")
                    .build()),
            3.0)
        .put(
            preventSharing(
                new ItemBuilder(new ItemStack(Material.BREAD))
                    .enchant(Enchantment.FIRE_ASPECT, 10)
                    .name("Very Hot Bread")
                    .build()),
            2.0)
        .put(
            preventSharing(
                new ItemBuilder(new ItemStack(Material.BREAD))
                    .enchant(Enchantment.KNOCKBACK, 10)
                    .name("Very Bouncy Bread")
                    .build()),
            1.0)
        .put(
            preventSharing(
                new ItemBuilder(new ItemStack(Material.BREAD))
                    .enchant(Enchantment.DAMAGE_ALL, 20)
                    .name("Insanely Sharp Bread")
                    .build()),
            1.0)
        .put(
            preventSharing(
                new ItemBuilder(new ItemStack(Material.BREAD))
                    .enchant(Enchantment.FIRE_ASPECT, 100)
                    .name("Insanely Hot Bread")
                    .build()),
            1.0)
        .put(
            preventSharing(
                new ItemBuilder(new ItemStack(Material.BREAD))
                    .enchant(Enchantment.KNOCKBACK, 100)
                    .name("Insanely Bouncy Bread")
                    .build()),
            1.0)
        .build();
  }

  static ImmutableMap<ItemStack, Double> getBadBreadsMap() {
    return new ImmutableMap.Builder<ItemStack, Double>()
        .put(
            preventSharing(
                new ItemBuilder(new ItemStack(Material.BREAD))
                    .enchant(Enchantment.FIRE_ASPECT, 1)
                    .name("Hot Bread")
                    .build()),
            20.0)
        .put(
            preventSharing(
                new ItemBuilder(new ItemStack(Material.BREAD))
                    .enchant(Enchantment.DAMAGE_ALL, 5)
                    .name("Sharp Bread")
                    .build()),
            20.0)
        .put(
            preventSharing(
                new ItemBuilder(new ItemStack(Material.BREAD))
                    .enchant(Enchantment.KNOCKBACK, 2)
                    .name("Bouncy Bread")
                    .build()),
            20.0)
        .build();
  }

  @Override
  public boolean canEnable(Set<Mutation> existingMutations) {
    return true;
  }

  @Override
  protected void givePlayerKit(MatchPlayer player, List<Kit> kits) {
    UUID playerId = player.getId();
    if (badBreadSet.contains(playerId)) {
      player.applyKit(
          new ItemKit(Maps.newHashMap(), Lists.newArrayList(badBreadChooser.choose(random))), true);
      badBreadSet.remove(playerId);
    } else {
      player.applyKit(
          new ItemKit(Maps.newHashMap(), Lists.newArrayList(breadChooser.choose(random))), true);
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onPlayerDamageByEntity(EntityDamageByEntityEvent event) {
    if (event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
      ItemStack itemInHand = ((Player) event.getDamager()).getItemInHand();
      Player hitPlayer = (Player) event.getEntity();
      if (itemInHand.isSimilar(POTION_BREAD)) {
        hitPlayer.addPotionEffect(
            new PotionEffect(
                potionChooser.choose(random), 20 * random.nextInt(7) + 3, random.nextInt(2) + 1));
      } else if (itemInHand.isSimilar(TELEPORT_BREAD)) {
        performBreadTeleport(hitPlayer);
      }
    }
  }

  /**
   * Teleport a player somewhere within 6 blocks.
   * Location will always be somewhere safe, and somewhere that the player could walk to
   */
  private void performBreadTeleport(Player hitPlayer) {
    Location origin = chooseStartingLocation(hitPlayer);

    Location previousLocation = copyLocation(origin, 0, 0, 0);
    PluginManager pluginManager = Bukkit.getPluginManager();
    Map<Location, Boolean> safeCache = new HashMap<>();

    int distance = random.nextInt(3) + 2;
    boolean locationChanged = false;
    for (int i = 0; i < 20; i++) {
      int dir = random.nextInt(4);
      int x = dir == 0 ? -1 : dir == 1 ? 1 : 0;
      int z = dir == 2 ? -1 : dir == 3 ? 1 : 0;

      Location nextLocation;
      for (int y = -2; y < 2; y++) {
        nextLocation = copyLocation(previousLocation, x, y, z).toBlockLocation();
        if (!safeCache.containsKey(nextLocation)) {
          // Check if the player can physically walk into the area?
          if (isSafe(nextLocation, y < 0 ? 2 - y : 2) && isSafe(previousLocation, y == 1 ? 3 : 2)) {
            // Check if pgm will deny entry
            PlayerCoarseMoveEvent event =
                new PlayerCoarseMoveEvent(
                    new PlayerMoveEvent(hitPlayer, previousLocation, nextLocation));
            pluginManager.callEvent(event);

            if (!event.isCancelled()) {
              previousLocation = nextLocation;
              safeCache.put(nextLocation, true);
              locationChanged = true;
            }
            safeCache.put(nextLocation, false);
            break;
          }
          safeCache.put(nextLocation, false);
        } else if (safeCache.get(nextLocation)) {
          previousLocation = nextLocation;
          locationChanged = true;
          break;
        }
      }
      if (origin.distance(previousLocation) > distance) {
        break;
      }
    }
    if (locationChanged) {
      hitPlayer.teleport(
          previousLocation.add(0.5, 0, 0.5), PlayerTeleportEvent.TeleportCause.ENDER_PEARL);
    }
  }

  private Location chooseStartingLocation(Player hitPlayer) {
    Location origin = hitPlayer.getLocation();

    if (!isSafe(origin, 2)) {
      Location firstTest = copyLocation(origin, 0, 0, 0).add(0, 0.5, 0);
      if (isSafe(firstTest, 2)) {
        return firstTest;
      } else {
        boolean found = false;
        for (int y = 0; y > -3 && !found; y--) {
          for (int x = -1; x < 2 && !found; x++) {
            for (int z = -1; z < 2 && !found; z++) {
              Location testLocation = copyLocation(origin, x, y, z);
              if (isSafe(testLocation, 2 - y)) {
                return testLocation;
              }
            }
          }
        }
      }
    }
    return origin;
  }

  @NotNull
  private static Location copyLocation(Location origin, int x, int y, int z) {
    Location possibleLocation =
        new Location(origin.getWorld(), origin.getX() + x, origin.getY() + y, origin.getZ() + z);
    possibleLocation.setYaw(origin.getYaw());
    possibleLocation.setPitch(origin.getPitch());
    return possibleLocation;
  }

  private boolean isSafe(Location location, int blockHeight) {
    if (!WorldBorders.isInsideBorder(location)) return false;

    Block block = location.getBlock();
    Block below = block.getRelative(BlockFace.DOWN);
    if (block.isLiquid() || BlockVectors.isSupportive(below.getType())) {
      Block previous = location.getBlock();
      for (int i = 0; i < blockHeight; i++) {
        if (previous.getType().isSolid()) {
          return false;
        }
        previous = previous.getRelative(BlockFace.UP);
      }
      return true;
    }
    return false;
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onPlayerDamage(EntityDamageEvent event) {
    if (event.getEntity() instanceof Player) {
      Player player = (Player) event.getEntity();
      if (player.getItemInHand().isSimilar(ARMORED_BREAD)) {
        event.setDamage(event.getDamage() * 0.5);
      }
    }
  }

  @EventHandler
  public void onPlayerSwitchTeams(PlayerPartyChangeEvent event) {
    if (!(event instanceof PlayerJoinMatchEvent)) {
      if (event.getNewParty() instanceof ObserverParty) {
        badBreadSet.add(event.getPlayer().getId());
      }
    }
  }
}

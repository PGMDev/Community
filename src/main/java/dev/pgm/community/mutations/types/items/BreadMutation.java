package dev.pgm.community.mutations.types.items;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dev.pgm.community.Community;
import dev.pgm.community.mutations.Mutation;
import dev.pgm.community.mutations.MutationType;
import dev.pgm.community.mutations.types.KitMutationBase;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.kits.ItemKit;
import tc.oc.pgm.kits.Kit;
import tc.oc.pgm.kits.tag.ItemTags;
import tc.oc.pgm.loot.WeightedRandomChooser;
import tc.oc.pgm.util.inventory.ItemBuilder;

public class BreadMutation extends KitMutationBase {
  static final ImmutableMap<PotionEffectType, Double> BAD_POTION_MAP =
      new ImmutableMap.Builder<PotionEffectType, Double>()
          .put(PotionEffectType.WEAKNESS, 10.0)
          .put(PotionEffectType.SLOW, 10.0)
          .put(PotionEffectType.POISON, 10.0)
          .put(PotionEffectType.WITHER, 10.0)
          .put(PotionEffectType.BLINDNESS, 5.0)
          .build();
  private static final ItemStack POTION_BREAD =
      preventSharing(
          new ItemBuilder(new ItemStack(Material.BREAD))
              .unbreakable(true)
              .name("Potion Bread")
              .build());
  static final ImmutableMap<ItemStack, Double> BREADS_MAP = getBreadsMap();
  private final WeightedRandomChooser<ItemStack> breadChooser;
  private final Random random;
  private WeightedRandomChooser<PotionEffectType> potionChooser;

  public BreadMutation(Match match) {
    super(match, MutationType.BREAD);

    random = Community.get().getRandom();
    breadChooser = new WeightedRandomChooser<>();
    breadChooser.addAll(BREADS_MAP);
    potionChooser = new WeightedRandomChooser<>();
    potionChooser.addAll(BAD_POTION_MAP);
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

  @Override
  public boolean canEnable(Set<Mutation> existingMutations) {
    return true;
  }

  @Override
  public List<Kit> getKits() {
    return Lists.newArrayList(
        new ItemKit(Maps.newHashMap(), Lists.newArrayList(breadChooser.choose(random))));
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onPlayerDamage(EntityDamageByEntityEvent event) {
    if (event.getDamager() instanceof Player
        && (((Player) event.getDamager()).getItemInHand()).isSimilar(POTION_BREAD)) {
      if (event.getEntity() instanceof LivingEntity) {
        ((LivingEntity) event.getEntity())
            .addPotionEffect(
                new PotionEffect(
                    potionChooser.choose(random), 20 * random.nextInt(7) + 3, random.nextInt(3)));
      }
    }
  }
}

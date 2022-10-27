package dev.pgm.community.mutations.types.items;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import dev.pgm.community.Community;
import dev.pgm.community.mutations.Mutation;
import dev.pgm.community.mutations.MutationType;
import dev.pgm.community.mutations.types.KitMutationBase;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.kits.ItemKit;
import tc.oc.pgm.kits.Kit;
import tc.oc.pgm.kits.tag.ItemTags;
import tc.oc.pgm.util.inventory.tag.ItemTag;

/** FireworkMutation - Provides extra firework effects in-game */
public class FireworkMutation extends KitMutationBase {

  // TODO: Blitz match: shoot off location fireworks to show where players are every X seconds after
  // match has been running for Y
  private static final String FIREWORK_METADATA = "mutation_firework";
  private static final ItemTag<String> FIREWORK_TAG = ItemTag.newString(FIREWORK_METADATA);
  private static final int FIREWORK_POWER = 2;

  private Cache<UUID, String> lastFirework =
      CacheBuilder.newBuilder().expireAfterWrite(2, TimeUnit.SECONDS).build();
  private Set<Firework> fireworks;

  private ScheduledFuture<?> task;

  public FireworkMutation(Match match) {
    super(match, MutationType.FIREWORK);
    this.fireworks = Sets.newHashSet();
  }

  @Override
  public void enable() {
    super.enable();
    task =
        match
            .getExecutor(MatchScope.RUNNING)
            .scheduleAtFixedRate(this::task, 0, 250, TimeUnit.MILLISECONDS);
  }

  @Override
  public void disable() {
    super.disable();
    this.fireworks.clear();
    if (task != null) {
      task.cancel(true);
      task = null;
    }
  }

  @Override
  public List<Kit> getKits() {
    return Lists.newArrayList(getFireworkKit());
  }

  public void task() {
    Iterator<Firework> iterator = fireworks.iterator();
    while (iterator.hasNext()) {
      Firework firework = iterator.next();
      if (firework.isDead()) {
        match
            .getExecutor(MatchScope.RUNNING)
            .schedule(() -> fling(firework), 30, TimeUnit.MILLISECONDS);
        iterator.remove();
      }
    }
  }

  private void fling(Firework firework) {
    if (!firework.hasMetadata(FIREWORK_METADATA)) return;
    if (firework.getPassenger() != null && firework.getPassenger() instanceof Player) {
      Player player = (Player) firework.getPassenger();
      Vector velocity = player.getLocation().getDirection().multiply(7).setY(0);
      player.setVelocity(velocity);
      player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 5, 5));
    }
  }

  @EventHandler
  public void onFireworkItemLaunch(PlayerInteractEvent event) {
    if (event.getItem() != null && event.getItem().getType() != Material.AIR) {
      String tag = FIREWORK_TAG.get(event.getItem());
      if (tag != null && tag.equalsIgnoreCase(FIREWORK_METADATA)) {
        Player player = event.getPlayer();

        FireworkMeta fireworkMeta = (FireworkMeta) event.getItem().getItemMeta();
        event.setCancelled(true);

        Firework firework =
            player.getLocation().getWorld().spawn(player.getLocation(), Firework.class);
        firework.setFireworkMeta(fireworkMeta);
        firework.setMetadata(FIREWORK_METADATA, new FixedMetadataValue(Community.get(), true));
        firework.setPassenger(player);

        player.getInventory().remove(event.getItem());
        player.updateInventory();

        fireworks.add(firework);
      }
    }
  }

  @EventHandler
  public void onFireworkArrow(ProjectileHitEvent event) {
    if (event.getEntity() == null) return;
    if (!(event.getEntity().getShooter() instanceof Player)) return;
    if (event.getEntity().getShooter() == null) return;

    Player shooter = (Player) event.getEntity().getShooter();

    if (lastFirework.getIfPresent(shooter.getUniqueId()) == null) {
      launchFirework(shooter, event.getEntity().getLocation());
    }
  }

  private static ItemKit getFireworkKit() {
    return new ItemKit(
        Maps.newHashMap(), Lists.newArrayList(getFirework(null, Type.BURST, FIREWORK_POWER)));
  }

  private static ItemStack getFirework(@Nullable Color color, Type type, int power) {
    Random random = Community.get().getRandom();
    ItemStack firework = new ItemStack(Material.FIREWORK);
    FireworkMeta meta = (FireworkMeta) firework.getItemMeta();
    meta.addEffect(
        FireworkEffect.builder()
            .withColor(
                color != null
                    ? color
                    : Color.fromBGR(random.nextInt(255), random.nextInt(255), random.nextInt(255)))
            .flicker(false)
            .trail(false)
            .with(type)
            .build());
    meta.setDisplayName(ChatColor.GREEN + "Mutation Firework");
    meta.setLore(Lists.newArrayList(ChatColor.GRAY + "Launch to make a quick getaway"));
    meta.addItemFlags(ItemFlag.values());
    meta.setPower(power);
    firework.setItemMeta(meta);
    FIREWORK_TAG.set(firework, FIREWORK_METADATA);
    ItemTags.PREVENT_SHARING.set(firework, true);
    return firework;
  }

  private void launchFirework(Player shooter, Location location) {
    lastFirework.put(shooter.getUniqueId(), "");
    Color color = null;
    MatchPlayer mp = match.getParticipant(shooter);
    if (mp != null) {
      color = mp.getParty().getFullColor();
    }
    Firework firework = location.getWorld().spawn(location, Firework.class);
    firework.setFireworkMeta((FireworkMeta) getFirework(color, Type.BURST, 0).getItemMeta());
  }

  @Override
  public boolean canEnable(Set<Mutation> existing) {
    return true;
  }
}

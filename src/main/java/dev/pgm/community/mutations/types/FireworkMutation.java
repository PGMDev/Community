package dev.pgm.community.mutations.types;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dev.pgm.community.Community;
import dev.pgm.community.mutations.MutationType;
import java.util.List;
import java.util.Random;
import java.util.UUID;
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
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.kits.ItemKit;
import tc.oc.pgm.kits.Kit;
import tc.oc.pgm.util.inventory.tag.ItemTag;

/** FireworkMutation - Provides extra firework effects in-game */
public class FireworkMutation extends KitMutationBase {

  // TODO: Blitz match: shoot off location fireworks to show where players are every X seconds after
  // match has been running for Y
  private static final String FIREWORK_METADATA = "mutation_firework";
  private static final ItemTag<String> FIREWORK_TAG = ItemTag.newString(FIREWORK_METADATA);

  private Cache<UUID, String> lastFirework =
      CacheBuilder.newBuilder().expireAfterWrite(2, TimeUnit.SECONDS).build();

  public FireworkMutation(Match match) {
    super(match, MutationType.FIREWORKS, getFireworkKit());
  }

  @Override
  public boolean canEnable() {
    return true;
  }

  @Override
  public List<Kit> getKits() {
    return Lists.newArrayList(getFireworkKit());
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
        firework.setPassenger(player);

        player.getInventory().remove(event.getItem());
        player.updateInventory();
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
    return new ItemKit(Maps.newHashMap(), Lists.newArrayList(getFirework(null, Type.BURST, 2)));
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
    meta.setLore(Lists.newArrayList(ChatColor.GRAY + "Launch me for a special surprise"));
    meta.addItemFlags(ItemFlag.values());
    firework.setItemMeta(meta);
    meta.setPower(power);
    FIREWORK_TAG.set(firework, FIREWORK_METADATA);
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
}

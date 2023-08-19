package dev.pgm.community.mutations.types.items;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dev.pgm.community.Community;
import dev.pgm.community.mutations.Mutation;
import dev.pgm.community.mutations.MutationType;
import dev.pgm.community.mutations.types.KitMutationBase;
import java.util.List;
import java.util.Set;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.kits.ItemKit;
import tc.oc.pgm.kits.Kit;
import tc.oc.pgm.kits.tag.ItemTags;
import tc.oc.pgm.util.bukkit.OnlinePlayerMapAdapter;
import tc.oc.pgm.util.inventory.ItemBuilder;
import tc.oc.pgm.util.inventory.tag.ItemTag;

public class GrapplingHookMutation extends KitMutationBase {

  private final OnlinePlayerMapAdapter<Long> playerCooldowns;

  private static final int COOLDOWN_MILISECONDS = 2000;
  public final String GRAPPLE_META = "grappling-hook";
  public final ItemTag<Boolean> GRAPPLE_META_TAG = ItemTag.newBoolean(GRAPPLE_META);

  public GrapplingHookMutation(Match match) {
    super(match, MutationType.GRAPPLING_HOOK);
    playerCooldowns = new OnlinePlayerMapAdapter<>(Community.get());
  }

  @Override
  public boolean canEnable(Set<Mutation> existingMutations) {
    return true;
  }

  @Override
  public List<Kit> getKits() {
    return Lists.newArrayList(new ItemKit(Maps.newHashMap(), Lists.newArrayList(getGrappleHook())));
  }

  public ItemStack getGrappleHook() {
    ItemStack grapple =
        new ItemBuilder()
            .material(Material.FISHING_ROD)
            .name(ChatColor.GREEN + "Grappling Hook")
            .lore(ChatColor.GRAY + "Launch, anchor, conquer!")
            .flags(ItemFlag.values())
            .unbreakable(true)
            .build();

    ItemTags.PREVENT_SHARING.set(grapple, true);
    GRAPPLE_META_TAG.set(grapple, true);

    return grapple;
  }

  @EventHandler(ignoreCancelled = true)
  public void onPlayerFishEvent(PlayerFishEvent event) {
    if (!GRAPPLE_META_TAG.has(event.getPlayer().getItemInHand())) return;

    // Set the hooks meta if the player has thrown the line
    if (event.getState() == PlayerFishEvent.State.FISHING) {
      event.getHook().setMetadata(GRAPPLE_META, new FixedMetadataValue(Community.get(), true));
    }

    // Only pull the player when reeling back in
    if (!(event.getState() == PlayerFishEvent.State.IN_GROUND
        || event.getState() == PlayerFishEvent.State.FAILED_ATTEMPT)) return;

    if (isPlayerOnCooldown(event.getPlayer())) return;

    // Calculate velocity to apply to player
    Location hookLocation = event.getHook().getLocation();
    Location playerLocation = event.getPlayer().getLocation();
    Vector direction = hookLocation.toVector().subtract(playerLocation.toVector());
    direction.multiply(0.5).setY(1);

    event.getPlayer().setVelocity(direction);
    event.getPlayer().playSound(playerLocation, Sound.BAT_TAKEOFF, 2, 1.2f);

    playerCooldowns.put(event.getPlayer(), System.currentTimeMillis());
  }

  @EventHandler(ignoreCancelled = true)
  public void onDamage(EntityDamageByEntityEvent event) {
    if (!(event.getDamager() instanceof FishHook)) return;
    FishHook fishHook = (FishHook) event.getDamager();
    if (!(fishHook.getShooter() instanceof Player)) return;

    // Cancel damage if caused by a grappling hook
    if (fishHook.hasMetadata(GRAPPLE_META)) event.setCancelled(true);
  }

  private boolean isPlayerOnCooldown(Player player) {
    Long lastGrapple = playerCooldowns.get(player);
    return (lastGrapple != null && lastGrapple > System.currentTimeMillis() - COOLDOWN_MILISECONDS);
  }
}

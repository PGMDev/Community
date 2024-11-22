package dev.pgm.community.mutations.types.arrows;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import dev.pgm.community.Community;
import dev.pgm.community.mutations.Mutation;
import dev.pgm.community.mutations.MutationType;
import dev.pgm.community.mutations.options.MutationOption;
import dev.pgm.community.mutations.options.MutationRangeOption;
import dev.pgm.community.mutations.types.BowMutation;
import dev.pgm.community.mutations.types.KitMutationBase;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.kits.ItemKit;
import tc.oc.pgm.kits.tag.ItemTags;
import tc.oc.pgm.util.inventory.ItemBuilder;

public class WebSlingersMutation extends KitMutationBase implements BowMutation {

  private static MutationRangeOption WEB_LIFE =
      new MutationRangeOption(
          "Web Life",
          "Length of time before webs are removed",
          MutationType.WEB_SLINGERS.getMaterial(),
          false,
          10,
          1,
          60);

  private int cleanupTask;

  private Map<Location, Long> webLocations = Maps.newHashMap();

  public WebSlingersMutation(Match match) {
    super(match, MutationType.WEB_SLINGERS, getWebBowKit());
    this.webLocations = Maps.newHashMap();
  }

  @Override
  public Collection<MutationOption> getOptions() {
    return Sets.newHashSet(WEB_LIFE);
  }

  @Override
  public void enable() {
    super.enable();
    this.cleanupTask =
        Community.get()
            .getServer()
            .getScheduler()
            .scheduleSyncRepeatingTask(Community.get(), this::cleanup, 0L, 20L);
  }

  @Override
  public void disable() {
    super.disable();
    this.webLocations.keySet().forEach(this::revertBlock);
    this.webLocations.clear();

    Community.get().getServer().getScheduler().cancelTask(cleanupTask);
  }

  @Override
  public boolean canEnable(Set<Mutation> existing) {
    return !hasBowMutation(existing);
  }

  @Override
  public ItemStack[] getAllItems() {
    return new ItemStack[] {getWebBow()};
  }

  @EventHandler
  public void onShootWeb(final EntityShootBowEvent event) {
    if (!(event.getEntity() instanceof Player)) return;
    Player player = (Player) event.getEntity();
    if (player == null || match.getParticipant(player) == null) return;
    ItemStack bow = event.getBow();

    if (!bow.isSimilar(getWebBow())) return;
    FallingBlock web =
        event
            .getEntity()
            .getWorld()
            .spawnFallingBlock(event.getProjectile().getLocation(), Material.WEB, (byte) 0);
    web.setDropItem(false);
    web.setVelocity(event.getProjectile().getVelocity());
    event.setProjectile(web);
  }

  @EventHandler
  public void onWebLand(final EntityChangeBlockEvent event) {
    if (!(event.getEntity() instanceof FallingBlock)) return;
    FallingBlock block = (FallingBlock) event.getEntity();
    if (block.getMaterial() != Material.WEB) return;

    Location location = block.getLocation();
    location.getBlock().setType(Material.WEB);
    webLocations.put(location, getDelayedTime(WEB_LIFE.getValue()));
  }

  private void cleanup() {
    Iterator<Entry<Location, Long>> e = webLocations.entrySet().iterator();
    while (e.hasNext()) {
      Entry<Location, Long> block = e.next();
      if (block.getValue() <= getDelayedTime(0)) {
        revertBlock(block.getKey());
        e.remove();
      }
    }
  }

  private void revertBlock(Location location) {
    if (location.getBlock().getType() == Material.WEB) {
      location.getBlock().setType(Material.AIR);
    }
  }

  private static ItemKit getWebBowKit() {
    return new ItemKit(
        Maps.newHashMap(), Lists.newArrayList(getWebBow(), new ItemStack(Material.ARROW)));
  }

  private static ItemStack getWebBow() {
    ItemStack bow =
        new ItemBuilder()
            .material(Material.BOW)
            .enchant(Enchantment.ARROW_INFINITE, 1)
            .name(ChatColor.DARK_RED + ChatColor.BOLD.toString() + "Web-Slinger")
            .lore(ChatColor.GRAY + "Use to launch webs")
            .flags(ItemFlag.values())
            .unbreakable(true)
            .build();

    ItemTags.PREVENT_SHARING.set(bow, true);

    return bow;
  }

  private long getDelayedTime(int delay) {
    return (System.currentTimeMillis() / 1000) + delay;
  }
}

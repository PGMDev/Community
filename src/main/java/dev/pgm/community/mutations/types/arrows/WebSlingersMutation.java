package dev.pgm.community.mutations.types.arrows;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dev.pgm.community.mutations.Mutation;
import dev.pgm.community.mutations.MutationType;
import dev.pgm.community.mutations.types.BowMutation;
import dev.pgm.community.mutations.types.KitMutationBase;
import java.util.Set;
import org.bukkit.ChatColor;
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

  public WebSlingersMutation(Match match) {
    super(match, MutationType.WEB_SLINGERS, getWebBowKit());
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

    event.getEntity().getLocation().getBlock().setType(Material.WEB);
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
}

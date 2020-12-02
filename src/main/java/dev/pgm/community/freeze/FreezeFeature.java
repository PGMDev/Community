package dev.pgm.community.freeze;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.feature.FeatureBase;
import dev.pgm.community.utils.CommandAudience;
import dev.pgm.community.utils.PGMUtils;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.player.event.ObserverInteractEvent;
import tc.oc.pgm.spawns.events.ObserverKitApplyEvent;
import tc.oc.pgm.util.text.TextTranslations;

public class FreezeFeature extends FeatureBase {

  private static final Material TOOL_MATERIAL = Material.ICE;
  private static final int TOOL_SLOT_NUM = 6;

  private final FreezeManager freeze;

  public FreezeFeature(Configuration config, Logger logger) {
    super(new FreezeConfig(config), logger);
    this.freeze = new FreezeManager();

    if (getConfig().isEnabled()) {
      enable();
    }
  }

  public FreezeConfig getFreezeConfig() {
    return (FreezeConfig) getConfig();
  }

  @Override
  public Set<CommunityCommand> getCommands() {
    return getConfig().isEnabled() ? Sets.newHashSet(new FreezeCommand()) : Sets.newHashSet();
  }

  public boolean isFrozen(Player player) {
    return freeze.isFrozen(player);
  }

  public int getFrozenAllPlayerCount() {
    return freeze.getOnlineCount() + freeze.getOfflineCount();
  }

  public int getOnlineCount() {
    return freeze.getOnlineCount();
  }

  public int getOfflineCount() {
    return freeze.getOfflineCount();
  }

  public List<Player> getFrozenPlayers() {
    return freeze.getFrozenPlayers();
  }

  public String getOfflineFrozenNames() {
    return freeze.getOfflineFrozenNames();
  }

  public void setFrozen(CommandAudience sender, Player freezee, boolean frozen, boolean silent) {
    freeze.setFrozen(
        sender,
        freezee,
        frozen,
        silent,
        PGMUtils.isPGMEnabled() && getFreezeConfig().isIntegrationEnabled());
  }

  private ItemStack getFreezeTool(CommandSender viewer) {
    ItemStack stack = new ItemStack(TOOL_MATERIAL);
    ItemMeta meta = stack.getItemMeta();
    meta.setDisplayName(
        ChatColor.WHITE
            + ChatColor.BOLD.toString()
            + TextTranslations.translate("moderation.freeze.itemName", viewer));
    meta.addItemFlags(ItemFlag.values());
    meta.setLore(
        Collections.singletonList(
            ChatColor.GRAY
                + TextTranslations.translate("moderation.freeze.itemDescription", viewer)));
    stack.setItemMeta(meta);
    return stack;
  }

  @EventHandler
  public void giveKit(final ObserverKitApplyEvent event) {
    if (!PGMUtils.isPGMEnabled() || !getFreezeConfig().isIntegrationEnabled()) return;
    Player player = event.getPlayer().getBukkit();
    if (player.hasPermission(CommunityPermissions.FREEZE)) {
      player.getInventory().setItem(TOOL_SLOT_NUM, getFreezeTool(player));
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onObserverToolFreeze(final ObserverInteractEvent event) {
    if (!PGMUtils.isPGMEnabled() || !getFreezeConfig().isIntegrationEnabled()) return;
    if (event.getPlayer().isDead()) return;

    if (freeze.isFrozen(event.getPlayer().getBukkit())) {
      event.setCancelled(true);
    } else {
      if (event.getClickedItem() != null
          && event.getClickedItem().getType() == TOOL_MATERIAL
          && event.getPlayer().getBukkit().hasPermission(CommunityPermissions.FREEZE)
          && event.getClickedPlayer() != null) {

        event.setCancelled(true);

        setFrozen(
            new CommandAudience(event.getPlayer().getBukkit()),
            event.getClickedPlayer().getBukkit(),
            !freeze.isFrozen(event.getClickedEntity()),
            event.getPlayer().isVanished());
      }
    }
  }

  private static final List<String> ALLOWED_CMDS = Lists.newArrayList("/msg", "/r", "/tell");

  @EventHandler(priority = EventPriority.HIGH)
  public void onPlayerCommand(final PlayerCommandPreprocessEvent event) {
    if (freeze.isFrozen(event.getPlayer())
        && !event.getPlayer().hasPermission(CommunityPermissions.FREEZE)) {
      boolean allow =
          ALLOWED_CMDS.stream()
              .filter(cmd -> event.getMessage().startsWith(cmd))
              .findAny()
              .isPresent();

      if (!allow) {
        // Don't allow commands except for those related to chat.
        event.setCancelled(true);
      }
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onPlayerMove(final PlayerMoveEvent event) {
    if (freeze.isFrozen(event.getPlayer())) {
      Location old = event.getFrom();
      old.setPitch(event.getTo().getPitch());
      old.setYaw(event.getTo().getYaw());
      event.setTo(old);
    }
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onVehicleMove(final VehicleMoveEvent event) {
    if (!event.getVehicle().isEmpty() && freeze.isFrozen(event.getVehicle().getPassenger())) {
      event.getVehicle().setVelocity(new Vector(0, 0, 0));
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onVehicleEnter(final VehicleEnterEvent event) {
    if (freeze.isFrozen(event.getEntered())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onVehicleExit(final VehicleExitEvent event) {
    if (freeze.isFrozen(event.getExited())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onVehicleDamage(final VehicleDamageEvent event) {
    if (freeze.isFrozen(event.getAttacker())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onBlockBreak(final BlockBreakEvent event) {
    if (freeze.isFrozen(event.getPlayer())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onBlockPlace(final BlockPlaceEvent event) {
    if (freeze.isFrozen(event.getPlayer())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onBucketFill(final PlayerBucketFillEvent event) {
    if (freeze.isFrozen(event.getPlayer())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onBucketEmpty(final PlayerBucketEmptyEvent event) {
    if (freeze.isFrozen(event.getPlayer())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOW) // ignoreCancelled doesn't seem to work well here
  public void onPlayerInteract(final PlayerInteractEvent event) {
    if (freeze.isFrozen(event.getPlayer())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onInventoryClick(final InventoryClickEvent event) {
    if (event.getWhoClicked() instanceof Player) {
      if (freeze.isFrozen(event.getWhoClicked())) {
        event.setCancelled(true);
      }
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onPlayerDropItem(final PlayerDropItemEvent event) {
    if (freeze.isFrozen(event.getPlayer())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onEntityDamge(final EntityDamageByEntityEvent event) {
    if (freeze.isFrozen(event.getDamager())) {
      event.setCancelled(true);
    }
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    if (freeze.isCached(event.getPlayer().getUniqueId())) {
      freeze.removeCachedPlayer(event.getPlayer().getUniqueId());
      setFrozen(CommandAudience.CONSOLE, event.getPlayer(), true, true);
    }
  }

  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent event) {
    if (freeze.isFrozen(event.getPlayer())) {
      freeze.cachePlayer(event.getPlayer());
    }
  }

  @EventHandler
  public void onPlayerKick(PlayerKickEvent event) {
    if (freeze.isFrozen(event.getPlayer())) {
      setFrozen(CommandAudience.CONSOLE, event.getPlayer(), false, true);
    }
  }
}

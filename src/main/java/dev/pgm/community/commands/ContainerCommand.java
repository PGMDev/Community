package dev.pgm.community.commands;

import static net.kyori.adventure.text.Component.text;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import com.google.common.collect.Sets;
import dev.pgm.community.Community;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.utils.CommandAudience;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryHolder;
import tc.oc.pgm.util.Audience;

public class ContainerCommand extends CommunityCommand implements Listener {

  private Set<UUID> clickingPlayers = Sets.newHashSet();
  private Set<UUID> editingPlayers = Sets.newHashSet();

  private static final Component EXIT_MESSAGE =
      text("Exited container editing mode", NamedTextColor.GRAY);

  public ContainerCommand() {
    Community.get().getServer().getPluginManager().registerEvents(this, Community.get());
  }

  @CommandAlias("chestedit|ce|cedit|containeredit")
  @Description("Edit inventory contents of target block")
  @CommandPermission(CommunityPermissions.CONTAINER)
  public void containerEditCommand(CommandAudience audience, Player player) {
    if (isChoosing(player)) {
      clickingPlayers.remove(player.getUniqueId());
      audience.sendWarning(EXIT_MESSAGE);
    } else {
      clickingPlayers.add(player.getUniqueId());
      audience.sendWarning(text("Left-Click a container to start editing", NamedTextColor.GRAY));
    }
  }

  private boolean isChoosing(Player player) {
    if (player == null) return false;
    return clickingPlayers.contains(player.getUniqueId());
  }

  private boolean canOpen(Block block) {
    return block != null && isTypeAllowed(block.getType());
  }

  private boolean isTypeAllowed(Material type) {
    switch (type) {
      case CHEST:
      case ENDER_CHEST:
      case FURNACE:
      case DISPENSER:
      case BEACON:
        return true;
      default:
        return false;
    }
  }

  private Component formatCoords(Block block, NamedTextColor coordColor) {
    Location loc = block.getLocation();
    int x = loc.getBlockX();
    int y = loc.getBlockY();
    int z = loc.getBlockZ();
    return text()
        .append(text("("))
        .append(text(x, coordColor))
        .append(text(", "))
        .append(text(y, coordColor))
        .append(text(", "))
        .append(text(z, coordColor))
        .append(text(")"))
        .color(NamedTextColor.GRAY)
        .build();
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onContainerClick(PlayerInteractEvent event) {
    Player player = event.getPlayer();
    Audience viewer = Audience.get(player);
    Block block = event.getClickedBlock();

    if (isChoosing(player) && canOpen(block) && event.getAction() == Action.LEFT_CLICK_BLOCK) {
      event.setCancelled(true);
      viewer.sendMessage(
          text()
              .append(text("Now opening container at ", NamedTextColor.GRAY))
              .append(formatCoords(block, NamedTextColor.GREEN)));

      InventoryHolder container = (InventoryHolder) block.getState();
      editingPlayers.add(player.getUniqueId());
      player.openInventory(container.getInventory());
      clickingPlayers.remove(player.getUniqueId());
    }
  }

  @EventHandler(ignoreCancelled = false, priority = EventPriority.MONITOR)
  public void onInventoryClickEvent(InventoryClickEvent event) {
    if (event.getWhoClicked() != null && event.getWhoClicked() instanceof Player) {
      Player player = (Player) event.getWhoClicked();
      if (editingPlayers.contains(player.getUniqueId())) {
        event.setCancelled(false);
      }
    }
  }

  @EventHandler(ignoreCancelled = false, priority = EventPriority.MONITOR)
  public void onInventoryClose(InventoryCloseEvent event) {
    if (event.getPlayer() != null && event.getPlayer() instanceof Player) {
      Player player = (Player) event.getPlayer();
      if (editingPlayers.contains(player.getUniqueId())) {
        editingPlayers.remove(player.getUniqueId());
        Audience.get(player).sendWarning(EXIT_MESSAGE);
      }
    }
  }
}

package dev.pgm.community.moderation.tools;

import static net.kyori.adventure.text.Component.text;
import static tc.oc.pgm.util.bukkit.BukkitUtils.colorize;
import static tc.oc.pgm.util.text.PlayerComponent.player;

import com.google.common.collect.Maps;
import dev.pgm.community.utils.Sounds;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.ObserverInteractEvent;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.inventory.ItemBuilder;
import tc.oc.pgm.util.named.NameStyle;

public class ModerationToolManager {

  private static final Material HOOK = Material.TRIPWIRE_HOOK;
  public static final ItemStack TP_HOOK =
      new ItemBuilder()
          .material(HOOK)
          .name(colorize("&c&lPlayer Hook"))
          .lore(colorize("&7Right-click to target player"), colorize("&7Left-Click to teleport"))
          .build();

  private Map<UUID, UUID> hooks;

  public ModerationToolManager() {
    this.hooks = Maps.newHashMap();
  }

  public void onInteract(ObserverInteractEvent event) {
    if (event.getClickedItem() != null) {
      ItemStack tool = event.getClickedItem();
      // TP HOOK TOOL
      if (tool.isSimilar(TP_HOOK)) {
        if (event.getClickType() == ClickType.RIGHT) {
          // Target the player
          if (event.getClickedPlayer() != null) {
            MatchPlayer target = event.getClickedPlayer();
            targetPlayer(event.getPlayer().getBukkit(), target.getBukkit());
            event.setCancelled(true);
          }
        } else {
          // Teleport
          teleportPlayer(event.getPlayer().getBukkit());
        }
      }
    }
  }

  public void giveTools(Player player) {
    player.getInventory().setItem(3, TP_HOOK);
  }

  private void teleportPlayer(Player sender) {
    Audience viewer = Audience.get(sender);
    if (!hooks.containsKey(sender.getUniqueId())) {
      viewer.sendWarning(text("No target selected! Right-click a player to target them"));
    } else {
      Player target = Bukkit.getPlayer(hooks.get(sender.getUniqueId()));
      if (target == null) {
        viewer.sendWarning(text("Target is no longer online"));
      } else {
        sender.teleport(target.getLocation());
        viewer.playSound(Sounds.TELEPORT);
      }
    }
  }

  public void targetPlayer(Player sender, Player target) {
    this.hooks.put(sender.getUniqueId(), target.getUniqueId());

    Audience viewer = Audience.get(sender);
    Component targetConfirmation =
        text()
            .append(text("You are now targeting "))
            .append(player(target, NameStyle.FANCY))
            .build();
    viewer.sendMessage(targetConfirmation);
    viewer.playSound(Sounds.TARGET_CONFIRM);
  }
}

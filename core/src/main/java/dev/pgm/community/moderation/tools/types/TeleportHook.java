package dev.pgm.community.moderation.tools.types;

import static net.kyori.adventure.text.Component.text;
import static tc.oc.pgm.util.player.PlayerComponent.player;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dev.pgm.community.moderation.tools.ToolBase;
import dev.pgm.community.moderation.tools.menu.TeleportTargetMenu;
import dev.pgm.community.utils.Sounds;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.ObserverInteractEvent;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.named.NameStyle;

public class TeleportHook extends ToolBase {

  private static final Material MATERIAL = Material.TRIPWIRE_HOOK;

  private static final String NAME = "&c&lPlayer Hook";
  private static final List<String> LORE =
      Lists.newArrayList(
          "&7Right-click &bplayer &7to select target",
          "&7Right-click &bair &7to open menu",
          "&7Left-Click to &ateleport");

  private final Cache<UUID, String> clickCache;
  private final Map<UUID, UUID> hooks;
  private final TeleportTargetMenu menu;

  public TeleportHook(int slot, boolean enabled) {
    super(slot, enabled);
    this.hooks = Maps.newHashMap();
    this.clickCache = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.SECONDS).build();
    this.menu = new TeleportTargetMenu(this);
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public List<String> getLore() {
    return LORE;
  }

  @Override
  public Material getMaterial() {
    return MATERIAL;
  }

  @Override
  public void onLeftClick(ObserverInteractEvent event) {
    teleportPlayer(event.getPlayer().getBukkit());
  }

  @Override
  public void onRightClick(ObserverInteractEvent event) {
    if (event.getClickedPlayer() != null) {
      // Target a player if clicked
      MatchPlayer target = event.getClickedPlayer();
      targetPlayer(event.getPlayer().getBukkit(), target.getBukkit());
      clickCache.put(event.getPlayer().getId(), "");
    } else if (clickCache.getIfPresent(event.getPlayer().getId()) == null) {
      // Open GUI if no player is present
      menu.open(event.getPlayer().getBukkit());
    }
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

  public void unTarget(Player sender) {
    this.hooks.remove(sender.getUniqueId());
    Audience viewer = Audience.get(sender);

    Component unTargetConfirmation =
        text("You no longer have a teleport target set", NamedTextColor.GRAY);
    viewer.sendMessage(unTargetConfirmation);
  }

  public void targetPlayer(Player sender, Player target) {
    this.hooks.put(sender.getUniqueId(), target.getUniqueId());

    Audience viewer = Audience.get(sender);
    Component targetConfirmation =
        text()
            .append(text("You are now targeting "))
            .append(player(target, NameStyle.FANCY))
            .color(NamedTextColor.GRAY)
            .build();
    viewer.sendMessage(targetConfirmation);
    viewer.playSound(Sounds.TARGET_CONFIRM);
  }

  public boolean isTarget(Player viewer, Player target) {
    UUID targetID = hooks.get(viewer.getUniqueId());
    return targetID != null && target.getUniqueId().equals(targetID);
  }

  public boolean hasTarget(Player player) {
    return hooks.containsKey(player.getUniqueId());
  }
}

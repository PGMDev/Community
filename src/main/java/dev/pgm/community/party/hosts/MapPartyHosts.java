package dev.pgm.community.party.hosts;

import static net.kyori.adventure.text.Component.text;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import dev.pgm.community.Community;
import dev.pgm.community.utils.BroadcastUtils;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.nms.NMSHacks;
import tc.oc.pgm.util.player.PlayerComponent;
import tc.oc.pgm.util.skin.Skin;

public class MapPartyHosts {

  private final Permission hostPermission;

  private UUID mainHost; // The player who has control over the map party
  private final Set<UUID> subHosts; // Players who are also hosting
  private final Map<UUID, String> nameCache; // Cached names easy offline lookup
  private final Map<UUID, Skin> skinCache; // Cached skins for menus
  private final Map<Player, PermissionAttachment> permissions; // Stored permissions

  public MapPartyHosts(Player host, Permission hostPermission) {
    this(host.getUniqueId(), hostPermission);
    this.setupHostPlayer(host);
  }

  public MapPartyHosts(UUID host, Permission hostPermission) {
    this.mainHost = host;
    this.subHosts = Sets.newHashSet();
    this.nameCache = Maps.newHashMap();
    this.skinCache = Maps.newHashMap();
    this.hostPermission = hostPermission;
    this.permissions = Maps.newHashMap();
  }

  public void setMainHost(Player player) {
    if (this.mainHost != null) {
      broadcastHostTransfer(mainHost, player);
      invalidatePlayer(mainHost);
    }

    this.mainHost = player.getUniqueId();
    setupHostPlayer(player);
  }

  public void addSubHost(Player player) {
    this.subHosts.add(player.getUniqueId());
    setupHostPlayer(player);
    broadcastHostAdd(player);
  }

  public boolean removeSubHost(String name) {
    Entry<UUID, String> cachedEntry =
        nameCache.entrySet().stream()
            .filter(e -> e.getValue().equalsIgnoreCase(name))
            .findAny()
            .orElse(null);

    if (cachedEntry != null) {
      UUID playerId = cachedEntry.getKey();
      if (isSubHost(playerId)) {
        this.subHosts.remove(playerId);
        invalidatePlayer(playerId);
        broadcastHostRemoval(playerId);
        return true;
      }
    }

    return false;
  }

  public boolean isMainHost(UUID playerId) {
    return mainHost.equals(playerId);
  }

  public boolean isSubHost(UUID playerId) {
    return subHosts.contains(playerId);
  }

  public boolean isHost(UUID playerId) {
    return isMainHost(playerId) || isSubHost(playerId);
  }

  public UUID getMainHostId() {
    return mainHost;
  }

  public Set<UUID> getSubHostIds() {
    return subHosts;
  }

  public Set<String> getHostNames() {
    return this.nameCache.values().stream().collect(Collectors.toSet());
  }

  public String getCachedName(UUID playerId) {
    return this.nameCache.get(playerId);
  }

  public Skin getCachedSkin(UUID playerId) {
    return this.skinCache.get(playerId);
  }

  private void setupHostPlayer(Player player) {
    this.cachePlayerInfo(player);
    PermissionAttachment perm = player.addAttachment(Community.get());
    perm.setPermission(hostPermission, true);
    this.permissions.put(player, perm);
  }

  private void removeHostPlayer(Player player) {
    if (permissions.get(player) != null) {
      player.removeAttachment(permissions.get(player));
      permissions.remove(player);
    }
  }

  public void onLogin(Player player) {
    if (isHost(player.getUniqueId())) {
      this.setupHostPlayer(player);
    }
  }

  public void onQuit(Player player) {
    if (isHost(player.getUniqueId())) {
      this.removeHostPlayer(player);
    }
  }

  private void cachePlayerInfo(Player player) {
    this.nameCache.put(player.getUniqueId(), player.getName());
    this.skinCache.put(player.getUniqueId(), NMSHacks.getPlayerSkin(player));
  }

  private void invalidatePlayer(UUID playerId) {
    Player bukkit = Bukkit.getPlayer(playerId);
    if (bukkit != null) {
      removeHostPlayer(bukkit);
    }
  }

  private void broadcastHostAdd(Player newPlayer) {
    Component newHostName = PlayerComponent.player(newPlayer, NameStyle.FANCY);
    BroadcastUtils.sendAdminChatMessage(
        text()
            .append(newHostName)
            .append(text(" has been added as a party host"))
            .color(NamedTextColor.GRAY)
            .clickEvent(ClickEvent.runCommand("/event hosts"))
            .hoverEvent(HoverEvent.showText(text("Click to view event hosts", NamedTextColor.GRAY)))
            .build());
  }

  private void broadcastHostRemoval(UUID hostId) {
    Component oldHostname = PlayerComponent.player(hostId, NameStyle.FANCY);
    BroadcastUtils.sendAdminChatMessage(
        text()
            .append(oldHostname)
            .append(text(" is no longer a party host"))
            .color(NamedTextColor.GRAY)
            .clickEvent(ClickEvent.runCommand("/event hosts"))
            .hoverEvent(HoverEvent.showText(text("Click to view event hosts", NamedTextColor.GRAY)))
            .build());
  }

  private void broadcastHostTransfer(UUID oldHost, Player newHost) {
    Component oldHostName = PlayerComponent.player(mainHost, NameStyle.FANCY);
    Component newHostName = PlayerComponent.player(newHost, NameStyle.FANCY);
    BroadcastUtils.sendAdminChatMessage(
        text()
            .append(text("Event ownership has been transferred from "))
            .append(oldHostName)
            .append(text(" to "))
            .append(newHostName)
            .color(NamedTextColor.GRAY)
            .hoverEvent(HoverEvent.showText(text("Click to view event hosts", NamedTextColor.GRAY)))
            .clickEvent(ClickEvent.runCommand("/event hosts"))
            .build());
  }
}

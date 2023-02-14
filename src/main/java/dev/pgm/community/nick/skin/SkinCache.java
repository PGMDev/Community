package dev.pgm.community.nick.skin;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;
import dev.pgm.community.Community;
import dev.pgm.community.CommunityPermissions;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Skin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.event.NameDecorationChangeEvent;
import tc.oc.pgm.api.integration.Integration;
import tc.oc.pgm.api.player.MatchPlayer;

public class SkinCache implements Listener {

  private final Cache<UUID, Skin> offlineSkins =
      CacheBuilder.newBuilder().maximumSize(500).expireAfterWrite(6, TimeUnit.HOURS).build();
  private final Random random = new Random();

  private final Map<UUID, Skin> customSkins = Maps.newHashMap();

  // TODO: NEEDS WORK! Backup skins when 0 are online, prevent duplicates, etc
  private Skin getRandomSkin() {
    if (offlineSkins.size() == 0) {
      return Skin.EMPTY; // TODO: Warning, this may be bad for 1.16 clients...
    }
    List<Skin> skins = offlineSkins.asMap().values().stream().collect(Collectors.toList());
    return skins.get(random.nextInt(skins.size()));
  }

  private Skin getSkin(Player player) {
    if (customSkins.containsKey(player.getUniqueId())) {
      return customSkins.get(player.getUniqueId());
    }
    return getRandomSkin();
  }

  private boolean canUseSkin(Player player) {
    return !player.hasPermission(Permissions.STAFF)
        && !player.hasPermission(Permissions.PREMIUM); // TODO: add specific node too
  }

  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent event) {
    Player player = event.getPlayer();
    if (canUseSkin(player)) {
      offlineSkins.put(player.getUniqueId(), player.getSkin());
    }
  }

  @EventHandler(priority = EventPriority.LOW)
  public void onPlayerJoin(PlayerJoinEvent event) {
    offlineSkins.invalidate(event.getPlayer().getUniqueId());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void refreshNamesOnLogin(PlayerJoinEvent event) {
    refreshPlayer(event.getPlayer());
  }

  // SPORTPAPER STUFF - TODO: Add alternative method and check if server is running SportPaper to
  // enable

  private void refreshAllViewers(Player player) {
    Bukkit.getOnlinePlayers().forEach(viewer -> refreshFakeName(player, viewer));
  }

  private void refreshPlayer(Player player) {
    final MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer(player);
    if (matchPlayer == null) return;

    // Update displayname
    player.setDisplayName(
        PGM.get()
            .getNameDecorationRegistry()
            .getDecoratedName(player, matchPlayer.getParty().getColor()));

    // for all other online players, refresh their views
    refreshAllViewers(player);

    // Refresh the view of the player
    refreshSelfView(player);

    // Reset visibility
    matchPlayer.resetVisibility();
  }

  private void refreshSelfView(Player viewer) {
    Bukkit.getOnlinePlayers().forEach(other -> refreshFakeName(other, viewer));
  }

  // TODO: Figure out how to use without SPORTPAPER API
  private void refreshFakeName(Player player, Player viewer) {
    boolean nicked = Integration.getNick(player) != null;
    boolean areFriends = Integration.isFriend(player, viewer);
    boolean canOverride = viewer.hasPermission(CommunityPermissions.NICKNAME_VIEW);

    boolean canSeeRealName = (canOverride || player == viewer || areFriends);

    if (nicked && !canSeeRealName) {
      String nick = Integration.getNick(player);
      MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer(player);
      String displayName =
          PGM.get()
              .getNameDecorationRegistry()
              .getDecoratedName(player, matchPlayer.getParty().getColor());
      player.setFakeDisplayName(viewer, displayName);
      player.setFakeNameAndSkin(viewer, nick, getSkin(player));
    } else {
      player.setFakeDisplayName(viewer, null);
      player.setFakeNameAndSkin(viewer, null, null);
    }
  }

  public void onSkinRefresh(Player player, Skin skin) {
    if (skin == null) {
      customSkins.remove(player.getUniqueId());
    }

    if (Integration.getNick(player) != null) {
      if (skin != null) {
        // Store custom skin for persistence
        customSkins.put(player.getUniqueId(), skin);
      }

      // Refresh skin for everyone online
      refreshPlayer(player);
    }

    // Let PGM know to refresh tab entry
    Community.get()
        .getServer()
        .getPluginManager()
        .callEvent(new NameDecorationChangeEvent(player.getUniqueId()));
  }
}

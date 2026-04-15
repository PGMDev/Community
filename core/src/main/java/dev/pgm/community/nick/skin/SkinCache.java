package dev.pgm.community.nick.skin;

import static dev.pgm.community.util.PlayerUtils.PLAYER_UTILS;

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
import org.bukkit.Bukkit;
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
import tc.oc.pgm.util.skin.Skin;

public class SkinCache implements Listener {

  private final Cache<UUID, Skin> offlineSkins = CacheBuilder.newBuilder()
      .maximumSize(500)
      .expireAfterWrite(6, TimeUnit.HOURS)
      .build();
  private final Random random = new Random();

  private final Map<UUID, Skin> customSkins = Maps.newHashMap();

  // TODO: NEEDS WORK! Backup skins when 0 are online, prevent duplicates, etc
  private Skin getRandomSkin() {
    if (offlineSkins.size() == 0) {
      // Sunny
      return new Skin(
          "ewogICJ0aW1lc3RhbXAiIDogMTY2NjM1NjEyNzE3MiwKICAicHJvZmlsZUlkIiA6ICI1MTY4ZjZlMjIyM2E0Y2FjYjdiN2QyZjYyZWMxZGFhOSIsCiAgInByb2ZpbGVOYW1lIiA6ICJkZWZfbm90X2FzaCIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9hM2JkMTYwNzlmNzY0Y2Q1NDFlMDcyZTg4OGZlNDM4ODVlNzExZjk4NjU4MzIzZGIwZjlhNjA0NWRhOTFlZTdhIgogICAgfQogIH0KfQ==",
          "gUaeJg93CpJwZm3QbT59cX9pKrT+KBEXSYoQFFyyYl3d/sEcPM/n4uRGFSZDJm6hD5qNpOIrD/Tdm9aW9224LXwoOhXTH4QjIy7m7ZH29oXwiUCs0UR/cFGOnUFaCF+8ggWYyf/UhUnTVfyZb/XonejaTI9+/WBQmuCbF7TcgGzvuhYaEb9mWxhEfBeaiHV1iMiEgo4NJVya0MKTaZ10jfqq09JgijbJidims4Y6Ep7ozvbcsDMjGK02/nzdZ6cq7eJ3w5ZanGrhVdvyV05mKfGGU3SaLwMZ4Yj/WtSO3ZC36KT9kMBWyTWjWDyIK+wYDhv9LTQ/XWezsnV0uJQv3ngy0yMZh/O+sQzsb3kGXlSzZQjWkhoCkgASS5P/dSTr4mHAgctnG96NczNJA2caYgone6ytGcet63Z5iGx23t+XYiFxK9xsEbJFSW0qpOOMxn1H/gH3b3lkJfsWt0kcxSNsbWWL5WdPEw6aN5TfAVUDnhtxNbtwXqqNcNDHkrvfWC9UQw5NFf41ytnBJRWVyAitz3u0+u7l0G2vfPtusEUkkiYElXCYT+dURnT41y5sbZ6FLh05J3WWLA4ZTHqpp3mEHbmV3NwbNWadTiXH3MmKBHgNT0Q3ZgENcdnaomTTEvOGsN8PymBYLUIDj3DfSp8yl/dCgy1jBiSPk+A+wgs=");
    }
    List<Skin> skins = offlineSkins.asMap().values().stream().toList();
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
      offlineSkins.put(player.getUniqueId(), PLAYER_UTILS.getPlayerSkin(player));
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
    player.setDisplayName(PGM.get()
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
    boolean canOverride = viewer.hasPermission(Permissions.STAFF)
        || viewer.hasPermission(CommunityPermissions.NICKNAME_VIEW);

    boolean canSeeRealName = (canOverride || player == viewer || areFriends);

    if (nicked && !canSeeRealName) {
      String nick = Integration.getNick(player);
      MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer(player);
      String displayName = PGM.get()
          .getNameDecorationRegistry()
          .getDecoratedName(player, matchPlayer.getParty().getColor());
      PLAYER_UTILS.setFakeNameAndSkin(player, viewer, displayName, nick, getSkin(player));
    } else {
      PLAYER_UTILS.setFakeNameAndSkin(player, viewer, null, null, null);
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

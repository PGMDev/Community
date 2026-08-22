package dev.pgm.community.nick.skin;

import static tc.oc.pgm.util.nms.PlayerUtils.PLAYER_UTILS;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import dev.pgm.community.Community;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.event.NameDecorationChangeEvent;
import tc.oc.pgm.api.integration.Integration;
import tc.oc.pgm.util.skin.Skin;

class SkinCache implements Listener {

  // Sunny
  private static final Skin DEFAULT_SKIN = new Skin(
      "ewogICJ0aW1lc3RhbXAiIDogMTY2NjM1NjEyNzE3MiwKICAicHJvZmlsZUlkIiA6ICI1MTY4ZjZlMjIyM2E0Y2FjYjdiN2QyZjYyZWMxZGFhOSIsCiAgInByb2ZpbGVOYW1lIiA6ICJkZWZfbm90X2FzaCIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9hM2JkMTYwNzlmNzY0Y2Q1NDFlMDcyZTg4OGZlNDM4ODVlNzExZjk4NjU4MzIzZGIwZjlhNjA0NWRhOTFlZTdhIgogICAgfQogIH0KfQ==",
      "gUaeJg93CpJwZm3QbT59cX9pKrT+KBEXSYoQFFyyYl3d/sEcPM/n4uRGFSZDJm6hD5qNpOIrD/Tdm9aW9224LXwoOhXTH4QjIy7m7ZH29oXwiUCs0UR/cFGOnUFaCF+8ggWYyf/UhUnTVfyZb/XonejaTI9+/WBQmuCbF7TcgGzvuhYaEb9mWxhEfBeaiHV1iMiEgo4NJVya0MKTaZ10jfqq09JgijbJidims4Y6Ep7ozvbcsDMjGK02/nzdZ6cq7eJ3w5ZanGrhVdvyV05mKfGGU3SaLwMZ4Yj/WtSO3ZC36KT9kMBWyTWjWDyIK+wYDhv9LTQ/XWezsnV0uJQv3ngy0yMZh/O+sQzsb3kGXlSzZQjWkhoCkgASS5P/dSTr4mHAgctnG96NczNJA2caYgone6ytGcet63Z5iGx23t+XYiFxK9xsEbJFSW0qpOOMxn1H/gH3b3lkJfsWt0kcxSNsbWWL5WdPEw6aN5TfAVUDnhtxNbtwXqqNcNDHkrvfWC9UQw5NFf41ytnBJRWVyAitz3u0+u7l0G2vfPtusEUkkiYElXCYT+dURnT41y5sbZ6FLh05J3WWLA4ZTHqpp3mEHbmV3NwbNWadTiXH3MmKBHgNT0Q3ZgENcdnaomTTEvOGsN8PymBYLUIDj3DfSp8yl/dCgy1jBiSPk+A+wgs=");

  private final Cache<UUID, Skin> offlineSkins = CacheBuilder.newBuilder()
      .maximumSize(500)
      .expireAfterWrite(6, TimeUnit.HOURS)
      .build();
  private final Random random = new Random();
  private final Map<UUID, Skin> customSkins = new ConcurrentHashMap<>();
  private final Map<UUID, Skin> assignedSkins = new ConcurrentHashMap<>();

  // TODO: NEEDS WORK! Backup skins when 0 are online, prevent duplicates, etc
  private Skin getRandomSkin() {
    Skin[] skins = offlineSkins.asMap().values().toArray(Skin[]::new);
    if (skins.length == 0) {
      return DEFAULT_SKIN;
    }
    return skins[random.nextInt(skins.length)];
  }

  public Skin getDisguiseSkin(Player player) {
    Skin custom = customSkins.get(player.getUniqueId());
    if (custom != null) return custom;

    return assignedSkins.computeIfAbsent(player.getUniqueId(), _ -> getRandomSkin());
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

    assignedSkins.remove(player.getUniqueId());
    customSkins.remove(player.getUniqueId());
  }

  @EventHandler(priority = EventPriority.LOW)
  public void onPlayerJoin(PlayerJoinEvent event) {
    offlineSkins.invalidate(event.getPlayer().getUniqueId());
  }

  public void onSkinRefresh(Player player, @Nullable Skin skin) {
    if (skin == null) {
      customSkins.remove(player.getUniqueId());
    } else {
      customSkins.put(player.getUniqueId(), skin);
    }

    if (Integration.getNick(player) != null && Community.get().isEnabled()) {
      new NameDecorationChangeEvent(player.getUniqueId()).callEvent();
    }
  }
}

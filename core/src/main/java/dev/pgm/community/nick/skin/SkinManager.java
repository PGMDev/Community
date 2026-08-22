package dev.pgm.community.nick.skin;

import dev.pgm.community.Community;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.util.skin.Skin;

public class SkinManager {

  private final SkinCache cache;
  private boolean registered;

  public SkinManager() {
    this.cache = new SkinCache();
    enable();
  }

  public void enable() {
    if (registered) return;

    Community.get().registerListener(cache);
    registered = true;
  }

  public void setSkin(Player player, @Nullable Skin skin) {
    cache.onSkinRefresh(player, skin);
  }

  public Skin getDisguiseSkin(Player player) {
    return cache.getDisguiseSkin(player);
  }

  public void disable() {
    if (registered) {
      HandlerList.unregisterAll(cache);
      registered = false;
    }
  }
}

package dev.pgm.community.nick.skin;

import static dev.pgm.community.nick.identity.PlayerIdentity.PLAYER_IDENTITY;

import dev.pgm.community.Community;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
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

  public void setSkin(Player player, Skin skin) {
    cache.onSkinRefresh(player, skin);
  }

  public void disable() {
    if (registered) {
      HandlerList.unregisterAll(cache);
      registered = false;
    }

    PLAYER_IDENTITY.clearAll();
  }
}

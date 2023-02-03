package dev.pgm.community.nick.skin;

import dev.pgm.community.Community;
import org.bukkit.Skin;
import org.bukkit.entity.Player;

public class SkinManager {

  private SkinCache cache;

  public SkinManager() {
    this.cache = new SkinCache();
    Community.get().registerListener(cache);
  }

  public void setSkin(Player player, Skin skin) {
    cache.onSkinRefresh(player, skin);
  }
}

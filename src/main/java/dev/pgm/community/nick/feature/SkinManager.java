package dev.pgm.community.nick.feature;

import dev.pgm.community.Community;
import dev.pgm.community.nick.skin.SkinCache;
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

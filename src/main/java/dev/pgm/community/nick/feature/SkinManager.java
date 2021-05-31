package dev.pgm.community.nick.feature;

import com.google.common.collect.Maps;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Skin;
import org.bukkit.entity.Player;
import tc.oc.pgm.util.event.player.PlayerSkinChangeEvent;

public class SkinManager {

  private Map<UUID, Skin> nickedSkins;

  public SkinManager() {
    this.nickedSkins = Maps.newHashMap();
  }

  public void setSkin(Player player, Skin skin) {
    this.nickedSkins.put(player.getUniqueId(), skin);
    Bukkit.getServer().getPluginManager().callEvent(new PlayerSkinChangeEvent(player, skin));
  }

  public Skin getSkin(Player player) {
    return nickedSkins.get(player.getUniqueId());
  }

  public void removeSkin(Player player) {
    this.nickedSkins.remove(player.getUniqueId());
    Bukkit.getServer().getPluginManager().callEvent(new PlayerSkinChangeEvent(player, null));
  }
}

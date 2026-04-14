package dev.pgm.community.util;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;
import tc.oc.pgm.util.skin.Skin;

public interface PlayerUtils {
  PlayerUtils PLAYER_UTILS = Platform.get(PlayerUtils.class);

  Skin getPlayerSkin(Player player);

  void setFakeNameAndSkin(Player player, Player viewer, String displayName, String nick, Skin skin);

  ItemStack customSkull(@NonNull String url, String displayName, String... lore);
}

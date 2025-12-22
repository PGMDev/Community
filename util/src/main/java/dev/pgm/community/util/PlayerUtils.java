package dev.pgm.community.util;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.util.skin.Skin;

public interface PlayerUtils {
  PlayerUtils PLAYER_UTILS = Platform.get(PlayerUtils.class);

  Skin getPlayerSkin(Player player);

  void setFakeNameAndSkin(Player player, Player viewer, String displayName, String nick, Skin skin);

  String getPlayerDisplayName(Player player, Player viewer);

  String getPlayerName(Player player, Player viewer);

  Skin getPlayerSkin(Player player, Player viewer);

  ItemStack customSkull(String url, String displayName, String... lore);
}

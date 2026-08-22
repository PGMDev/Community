package dev.pgm.community.util;

import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;

public interface PlayerUtils {
  PlayerUtils PLAYER_UTILS = Platform.get(PlayerUtils.class);

  ItemStack customSkull(@NonNull String url, String displayName, String... lore);
}

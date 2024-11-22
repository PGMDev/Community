package dev.pgm.community.util;

import org.bukkit.entity.FishHook;
import org.bukkit.event.player.PlayerFishEvent;

public interface EventUtils {
  EventUtils EVENT_UTILS = Platform.get(EventUtils.class);

  FishHook getFishHook(PlayerFishEvent fishEvent);
}

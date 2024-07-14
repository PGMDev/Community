package dev.pgm.community.platform.sportpaper;

import static tc.oc.pgm.util.platform.Supports.Priority.HIGH;
import static tc.oc.pgm.util.platform.Supports.Variant.SPORTPAPER;

import dev.pgm.community.platform.CommunityEvents;
import org.bukkit.entity.FishHook;
import org.bukkit.event.player.PlayerFishEvent;
import tc.oc.pgm.util.platform.Supports;

@Supports(value = SPORTPAPER, priority = HIGH)
public class SpEvents implements CommunityEvents {
  @Override
  public FishHook getFishHook(PlayerFishEvent event) {
    return event.getHook();
  }
}

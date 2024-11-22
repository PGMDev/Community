package dev.pgm.community.platform.sportpaper;

import static dev.pgm.community.util.Supports.Variant.SPORTPAPER;

import dev.pgm.community.util.EventUtils;
import dev.pgm.community.util.Supports;
import org.bukkit.entity.FishHook;
import org.bukkit.event.player.PlayerFishEvent;

@Supports(SPORTPAPER)
public class SpEventUtils implements EventUtils {
  @Override
  public FishHook getFishHook(PlayerFishEvent fishEvent) {
    return fishEvent.getHook();
  }
}

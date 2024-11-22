package dev.pgm.community.platform.modern;

import static dev.pgm.community.util.Supports.Variant.PAPER;

import dev.pgm.community.util.EventUtils;
import dev.pgm.community.util.Supports;
import org.bukkit.entity.FishHook;
import org.bukkit.event.player.PlayerFishEvent;

@Supports(value = PAPER, minVersion = "1.20.6")
public class ModernEventUtils implements EventUtils {
  @Override
  public FishHook getFishHook(PlayerFishEvent fishEvent) {
    return fishEvent.getHook();
  }
}

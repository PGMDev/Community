package dev.pgm.community.platform.modern;

import static tc.oc.pgm.util.platform.Supports.Variant.PAPER;

import dev.pgm.community.platform.CommunityEvents;
import org.bukkit.entity.FishHook;
import org.bukkit.event.player.PlayerFishEvent;
import tc.oc.pgm.util.platform.Supports;

@Supports(value = PAPER, minVersion = "1.20.6")
public class ModernEvents implements CommunityEvents {
  @Override
  public FishHook getFishHook(PlayerFishEvent event) {
    return event.getHook();
  }
}

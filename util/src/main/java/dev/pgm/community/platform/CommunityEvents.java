package dev.pgm.community.platform;

import static dev.pgm.community.platform.Reflections.INSTANCE;

import org.bukkit.entity.FishHook;
import org.bukkit.event.player.PlayerFishEvent;
import tc.oc.pgm.util.platform.Platform;

public interface CommunityEvents {
  CommunityEvents EVENTS = Platform.get(CommunityEvents.class, INSTANCE);

  FishHook getFishHook(PlayerFishEvent event);
}

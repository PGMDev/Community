package dev.pgm.community.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** The foundation for all Community Events */
public class CommunityEvent extends Event {

  private static final HandlerList handlers = new HandlerList();

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}

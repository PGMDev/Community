package dev.pgm.community.reports;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlayerReportEvent extends Event {

  private final Report report;

  public PlayerReportEvent(Report report) {
    this.report = report;
  }

  public Report getReport() {
    return report;
  }

  private static final HandlerList handlers = new HandlerList();

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}

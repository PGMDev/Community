package dev.pgm.community.reports.events;

import dev.pgm.community.events.CommunityEvent;
import dev.pgm.community.reports.Report;

public class PlayerReportEvent extends CommunityEvent {

  private final Report report;

  public PlayerReportEvent(Report report) {
    this.report = report;
  }

  public Report getReport() {
    return report;
  }
}

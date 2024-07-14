package dev.pgm.community.events;

import dev.pgm.community.assistance.Report;

/** PlayerReportEvent - Called when a report is created */
public class PlayerReportEvent extends CommunityEvent {

  private final Report report;

  public PlayerReportEvent(Report report) {
    this.report = report;
  }

  public Report getReport() {
    return report;
  }
}

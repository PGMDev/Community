package dev.pgm.community.assistance;

import java.time.Instant;
import java.util.UUID;

public class Report extends AssistanceRequest implements Comparable<Report> {

  private final UUID reportId;
  private boolean notifiedSender;

  /**
   * Report Holds information related to a report
   *
   * @param reportedId UUID of reported player
   * @param reporterId UUID of reporting player
   * @param reason reason for report
   * @param time time reported
   * @param name of current server
   */
  public Report(UUID reportedId, UUID reporterId, String reason, Instant time, String server) {
    this(UUID.randomUUID(), reportedId, reporterId, reason, time, server);
  }

  public Report(
      UUID reportId, UUID reportedId, UUID reporterId, String reason, Instant time, String server) {
    super(reporterId, reportedId, time, reason, server, RequestType.REPORT);
    this.reportId = reportId;
    this.notifiedSender = false;
  }

  /**
   * Get the {@link UUID} which identifies the report
   *
   * @return report id
   */
  public UUID getId() {
    return reportId;
  }

  public void setNotified(boolean notified) {
    this.notifiedSender = notified;
  }

  public boolean hasNotified() {
    return notifiedSender;
  }

  @Override
  public int compareTo(Report o) {
    return -getTime().compareTo(o.getTime());
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof Report)) return false;
    Report otherReport = (Report) other;
    return getId().equals(otherReport.getId());
  }
}

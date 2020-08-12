package dev.pgm.community.reports;

import java.time.Instant;
import java.util.UUID;

public class Report implements Comparable<Report> {

  private final UUID reportId;
  private final UUID reportedId;
  private final UUID reporterId;
  private final String reason;
  private final Instant time;

  /**
   * Report Holds information related to a report
   *
   * @param reportedId UUID of reported player
   * @param reporterId UUID of reporting player
   * @param reason reason for report
   * @param time time reported
   */
  public Report(UUID reportedId, UUID reporterId, String reason, Instant time) {
    this(UUID.randomUUID(), reportedId, reporterId, reason, time);
  }

  public Report(UUID reportId, UUID reportedId, UUID reporterId, String reason, Instant time) {
    this.reportId = reportId;
    this.reportedId = reportedId;
    this.reporterId = reporterId;
    this.reason = reason;
    this.time = time;
  }
  /**
   * Get the {@link UUID} which identifies the report
   *
   * @return report id
   */
  public UUID getId() {
    return reportId;
  }

  /**
   * Get the reported UUID
   *
   * @return A UUID of the reported
   */
  public UUID getReportedId() {
    return reportedId;
  }

  /**
   * Get the reporter UUID
   *
   * @return A UUID of the reporter
   */
  public UUID getReporterId() {
    return reporterId;
  }

  /**
   * Get the reason for the report
   *
   * @return A string reason for report
   */
  public String getReason() {
    return reason;
  }

  /**
   * Get the time of the report
   *
   * @return Time of the report
   */
  public Instant getTime() {
    return time;
  }

  @Override
  public String toString() {
    return String.format(
        "{id: %s, reported: %s, sender: %s, reason:%s, time%s}",
        getId().toString(),
        getReportedId().toString(),
        getReporterId().toString(),
        getReason(),
        getTime().toString());
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

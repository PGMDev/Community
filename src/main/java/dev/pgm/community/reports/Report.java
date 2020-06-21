package dev.pgm.community.reports;

import java.time.Instant;
import java.util.UUID;

public class Report {

  private UUID reportedUUID;
  private UUID reporterUUID;
  private String reason;
  private Instant time;

  /**
   * Report Holds information related to a report
   *
   * @param reportedUUID UUID of reported player
   * @param reporterUUID UUID of reporting player
   * @param reason reason for report
   * @param time time reported
   */
  public Report(UUID reportedUUID, UUID reporterUUID, String reason, Instant time) {
    this.reportedUUID = reportedUUID;
    this.reporterUUID = reporterUUID;
    this.reason = reason;
    this.time = time;
  }

  /**
   * Get the reported UUID
   *
   * @return A UUID of the reported
   */
  public UUID getReportedUUID() {
    return reportedUUID;
  }

  /**
   * Get the reporter UUID
   *
   * @return A UUID of the reporter
   */
  public UUID getReporterUUID() {
    return reporterUUID;
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
}

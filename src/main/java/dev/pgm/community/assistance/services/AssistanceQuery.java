package dev.pgm.community.assistance.services;

public interface AssistanceQuery {

  static final String TABLE_NAME = "reports";
  static final String TABLE_FIELDS =
      "(id VARCHAR(36) PRIMARY KEY, "
          + "sender VARCHAR(36), "
          + "reported VARCHAR(36), "
          + "reason VARCHAR(255), "
          + "time LONG, "
          + "server VARCHAR(255))";
  static final String INSERT_REPORT_QUERY =
      "INSERT INTO "
          + TABLE_NAME
          + "(id, sender, reported, reason, time, server) VALUES (?, ?, ?, ?, ?, ?)";
  static final String SELECT_REPORT_QUERY =
      "SELECT id, sender, reason, time FROM " + TABLE_NAME + " WHERE reported = ?";
}

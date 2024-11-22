package dev.pgm.community.sessions.services;

public interface SessionDataQuery {

  static final String TABLE_NAME = "sessions";
  static final String TABLE_FIELDS =
      "(id VARCHAR(36) PRIMARY KEY, player VARCHAR(36), disguised BOOL, server VARCHAR(32), start_time BIGINT, end_time BIGINT)";

  static final String INSERT_SESSION_QUERY =
      "INSERT INTO "
          + TABLE_NAME
          + "(id, player, disguised, server, start_time, end_time) VALUES (?, ?, ?, ?, ?, ?)";

  static final String SELECT_DISGUISED_SESSION_QUERY =
      "SELECT * from "
          + TABLE_NAME
          + " where player = ? AND disguised = 0 ORDER BY -end_time LIMIT 1";
  static final String SELECT_SESSION_QUERY =
      "SELECT * from " + TABLE_NAME + " where player = ? ORDER BY -end_time LIMIT 1";

  static final String UPDATE_SESSION_ENDTIME_QUERY =
      "UPDATE " + TABLE_NAME + " SET end_time = ? WHERE id = ?";

  static final String UPDATE_ONGOING_SESSION_ENDING_QUERY =
      "UPDATE " + TABLE_NAME + " SET end_time = ? WHERE server = ? AND end_time IS NULL";
}

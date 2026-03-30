package dev.pgm.community.sessions.services;

public interface SessionDataQuery {

  String TABLE_NAME = "sessions";
  String TABLE_FIELDS =
      "(id VARCHAR(36) PRIMARY KEY, player VARCHAR(36), disguised BOOL, server VARCHAR(32), start_time BIGINT, end_time BIGINT)";

  String INSERT_SESSION_QUERY = "INSERT INTO " + TABLE_NAME
      + "(id, player, disguised, server, start_time, end_time) VALUES (?, ?, ?, ?, ?, ?)";

  String UPDATE_SESSION_ENDTIME_QUERY = "UPDATE " + TABLE_NAME + " SET end_time = ? WHERE id = ?";

  String LATEST_TABLE_NAME = "latest_sessions";
  String LATEST_TABLE_FIELDS =
      "(player VARCHAR(36), ignore_disguised BOOL, session_id VARCHAR(36), disguised BOOL, server VARCHAR(32), start_time BIGINT, end_time BIGINT, PRIMARY KEY (player, ignore_disguised))";

  String SELECT_LATEST_SESSION_QUERY =
      "SELECT * FROM " + LATEST_TABLE_NAME + " WHERE player = ? AND ignore_disguised = ? LIMIT 1";

  String UPDATE_LATEST_ENDTIME_QUERY =
      "UPDATE " + LATEST_TABLE_NAME + " SET end_time = ? WHERE session_id = ?";
}

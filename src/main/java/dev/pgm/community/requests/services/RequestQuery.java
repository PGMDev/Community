package dev.pgm.community.requests.services;

public interface RequestQuery {

  static final String TABLE_FIELDS =
      "(id VARCHAR(36) PRIMARY KEY, last_request_time LONG, last_request_map VARCHAR(255), last_sponsor_time LONG, last_sponsor_map VARCHAR(255), tokens INT, last_token_refresh LONG)";
  static final String TABLE_NAME = "requests";

  static final String INSERT_REQUESTS_QUERY =
      "INSERT INTO " + TABLE_NAME + " VALUES (?,?,?,?,?,?,?)";

  static final String UPDATE_REQUEST_QUERY =
      "UPDATE "
          + TABLE_NAME
          + " SET last_request_time = ?, last_request_map = ?, last_sponsor_time = ?, last_sponsor_map = ?, tokens = ?, last_token_refresh = ? WHERE id = ? ";

  static final String SELECT_REQUEST_QUERY =
      "SELECT * from " + TABLE_NAME + " WHERE id = ? LIMIT 1";
}

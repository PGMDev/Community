package dev.pgm.community.nick.services;

public interface NickQuery {

  String TABLE_NAME = "nicknames";
  String TABLE_FIELDS =
      "(playerId VARCHAR(36) PRIMARY KEY, nickname VARCHAR(16), date LONG, enabled BOOL)";

  String INSERT_NICKNAME_QUERY =
      "INSERT INTO " + TABLE_NAME + "(playerId, nickname, date, enabled) VALUES (?,?,?,?)";

  String SELECT_NICKNAME_BY_ID_QUERY =
      "SELECT * from " + TABLE_NAME + " where playerId = ? LIMIT 1";

  String UPDATE_NICKNAME_QUERY =
      "UPDATE " + TABLE_NAME + " set nickname = ?, enabled = ?, date = ? where playerId = ?";

  String SELECT_NICKNAME_BY_NAME_QUERY =
      "SELECT * from " + TABLE_NAME + " where LOWER(nickname) = LOWER(?)";
}

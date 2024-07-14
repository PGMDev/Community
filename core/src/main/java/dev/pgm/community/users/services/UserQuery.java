package dev.pgm.community.users.services;

public interface UserQuery {

  static final String TABLE_FIELDS =
      "(id VARCHAR(36) PRIMARY KEY, name VARCHAR(16), first_join LONG, join_count INT)";
  static final String TABLE_NAME = "users";

  static final String INSERT_USER_QUERY =
      "INSERT INTO " + TABLE_NAME + "(id, name, first_join, join_count) VALUES (?,?,?,?)";

  static final String USERNAME_QUERY =
      "SELECT * from " + TABLE_NAME + " WHERE LOWER(name) = LOWER(?) LIMIT 1";
  static final String PLAYERID_QUERY = "SELECT * from " + TABLE_NAME + " WHERE id = ? LIMIT 1";

  static final String UPDATE_USER_QUERY =
      "UPDATE " + TABLE_NAME + " SET name = ?, join_count = ? WHERE id = ? ";
}

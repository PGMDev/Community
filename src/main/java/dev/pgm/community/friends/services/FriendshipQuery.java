package dev.pgm.community.friends.services;

public interface FriendshipQuery {

  static final String TABLE_NAME = "friendships";
  static final String TABLE_FIELDS =
      "(id VARCHAR(36) PRIMARY KEY, "
          + "requester VARCHAR(36), "
          + "requested VARCHAR(36), "
          + "status VARCHAR(8), "
          + "requestDate LONG, "
          + "updateDate LONG)";

  static final String INSERT_FRIENDSHIP_QUERY =
      "INSERT INTO "
          + TABLE_NAME
          + "(id, requester, requested, status, requestDate, updateDate) VALUES (?, ?, ?, ?, ?, ?)";

  static final String SELECT_FRIENDSHIPS_QUERY =
      "SELECT * from " + TABLE_NAME + " where (requester = ? OR requested = ?)";

  static final String UPDATE_FRIENDSHIP_QUERY =
      "UPDATE " + TABLE_NAME + " SET status = ?, updateDate = ? WHERE id = ? ";
}

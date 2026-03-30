package dev.pgm.community.friends.services;

public interface FriendshipQuery {

  String TABLE_NAME = "friendships";
  String TABLE_FIELDS = "(id VARCHAR(36) PRIMARY KEY, "
      + "requester VARCHAR(36), "
      + "requested VARCHAR(36), "
      + "status VARCHAR(8), "
      + "requestDate LONG, "
      + "updateDate LONG)";

  String INSERT_FRIENDSHIP_QUERY = "INSERT INTO "
      + TABLE_NAME
      + "(id, requester, requested, status, requestDate, updateDate) VALUES (?, ?, ?, ?, ?, ?)";

  String SELECT_FRIENDSHIPS_QUERY =
      "SELECT * from " + TABLE_NAME + " where (requester = ? OR requested = ?)";

  String UPDATE_FRIENDSHIP_QUERY =
      "UPDATE " + TABLE_NAME + " SET status = ?, updateDate = ? WHERE id = ? ";
}

package dev.pgm.community.moderation.services;

public interface ModerationQuery {

  String TABLE_NAME = "punishments";
  String TABLE_FIELDS =
      "(id VARCHAR(36) PRIMARY KEY, punished VARCHAR(36), issuer VARCHAR(36), reason VARCHAR(255), type VARCHAR(8), time LONG, expires LONG, active BOOL, last_updated LONG, updated_by VARCHAR(36), service VARCHAR(255))";

  String INSERT_PUNISHMENT_QUERY = "INSERT INTO "
      + TABLE_NAME
      + "(id, punished, issuer, reason, type, time, expires, active, last_updated, updated_by, service) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

  String SELECT_PUNISHMENTS_QUERY = "SELECT * from " + TABLE_NAME + " where punished = ?";

  String SELECT_ACTIVE_PUNISHMENTS_QUERY =
      "SELECT id, type, active, expires, time, service, reason, issuer from " + TABLE_NAME
          + " where punished = ? AND active = ?";

  String SINGLE_PARDON_TYPE = "AND type = ?";
  String MULTI_PARDON_TYPE = "AND (type = ? OR type = ? OR type = ?)";
  String PARDON_QUERY = "UPDATE "
      + TABLE_NAME
      + " SET active = ?, last_updated = ?, updated_by = ? WHERE active = ? AND punished = ? ";
  String DEACTIVATE_QUERY =
      "UPDATE " + TABLE_NAME + " SET active = ?  WHERE active = ? AND punished = ? ";

  String SELECT_RECENT_QUERY = "SELECT * from " + TABLE_NAME + " WHERE time > ? LIMIT ?";
}

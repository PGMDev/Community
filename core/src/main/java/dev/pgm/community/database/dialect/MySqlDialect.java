package dev.pgm.community.database.dialect;

public class MySqlDialect implements SqlDialect {

  @Override
  public String upsertLatestSessionQuery() {
    return "INSERT INTO latest_sessions"
        + "(player, ignore_disguised, session_id, disguised, server, start_time, end_time)"
        + " VALUES (?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE "
        + ifNewer("session_id") + ", " + ifNewer("disguised") + ", " + ifNewer("server") + ", "
        + ifNewer("end_time") + ", " + ifNewer("start_time");
  }

  private static String ifNewer(String column) {
    return column + " = IF(VALUES(start_time) >= start_time, VALUES(" + column + "), " + column
        + ")";
  }

  @Override
  public String createIndexQuery(String table, String indexName, String columns) {
    return String.format("CREATE INDEX %s ON %s (%s)", indexName, table, columns);
  }

  @Override
  public String findIndexQuery() {
    return "SELECT COUNT(*) FROM information_schema.statistics"
        + " WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?";
  }

  @Override
  public String upsertUserSettingQuery() {
    return "INSERT INTO user_settings (id, setting, setting_value) VALUES (?, ?, ?)"
        + " ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value)";
  }

  @Override
  public String transactionalTableOptions() {
    return " ENGINE=InnoDB";
  }

  @Override
  public String selectForUpdateClause() {
    return " FOR UPDATE";
  }

  @Override
  public String findNonTransactionalTableQuery() {
    return "SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() "
        + "AND TABLE_NAME = ? AND ENGINE <> 'InnoDB'";
  }

  @Override
  public boolean reportsMatchedRows() {
    return false;
  }
}

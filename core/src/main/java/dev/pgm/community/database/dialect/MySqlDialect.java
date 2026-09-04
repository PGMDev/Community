package dev.pgm.community.database.dialect;

public class MySqlDialect implements SqlDialect {

  @Override
  public String upsertLatestSessionQuery() {
    return "INSERT INTO latest_sessions"
        + "(player, ignore_disguised, session_id, disguised, server, start_time, end_time)"
        + " VALUES (?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE "
        + "session_id = VALUES(session_id), disguised = VALUES(disguised), "
        + "server = VALUES(server), start_time = VALUES(start_time), end_time = VALUES(end_time)";
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
}

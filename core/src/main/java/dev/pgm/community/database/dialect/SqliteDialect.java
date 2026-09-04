package dev.pgm.community.database.dialect;

public class SqliteDialect implements SqlDialect {

  @Override
  public String upsertLatestSessionQuery() {
    return "INSERT INTO latest_sessions"
        + "(player, ignore_disguised, session_id, disguised, server, start_time, end_time)"
        + " VALUES (?, ?, ?, ?, ?, ?, ?) ON CONFLICT(player, ignore_disguised) DO UPDATE SET "
        + "session_id = excluded.session_id, disguised = excluded.disguised, "
        + "server = excluded.server, start_time = excluded.start_time, end_time = excluded.end_time";
  }

  @Override
  public String createIndexQuery(String table, String indexName, String columns) {
    return String.format(
        "CREATE INDEX IF NOT EXISTS %s_%s ON %s (%s)", table, indexName, table, columns);
  }

  @Override
  public String findIndexQuery() {
    return null; // CREATE INDEX IF NOT EXISTS is idempotent
  }

  @Override
  public String upsertUserSettingQuery() {
    return "INSERT INTO user_settings (id, setting, setting_value) VALUES (?, ?, ?)"
        + " ON CONFLICT(id, setting) DO UPDATE SET setting_value = excluded.setting_value";
  }
}

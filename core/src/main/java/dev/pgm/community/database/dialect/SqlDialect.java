package dev.pgm.community.database.dialect;

public interface SqlDialect {

  String upsertLatestSessionQuery();

  String upsertUserSettingQuery();

  String createIndexQuery(String table, String indexName, String columns);

  String findIndexQuery();
}

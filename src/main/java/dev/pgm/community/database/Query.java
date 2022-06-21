package dev.pgm.community.database;

import static com.google.common.base.Preconditions.checkNotNull;

public class Query {

  public static String createTable(String tableName, String fields) {
    checkNotNull(tableName);
    checkNotNull(fields);

    return String.format("CREATE TABLE IF NOT EXISTS %s %s", tableName, fields);
  }

  public static String countTable(String tableName) {
    checkNotNull(tableName);
    return String.format("SELECT count(*) from %s", tableName);
  }
}

package dev.pgm.community.database;

import static tc.oc.pgm.util.Assert.assertNotNull;

public class Query {

  public static String createTable(String tableName, String fields) {
    assertNotNull(tableName);
    assertNotNull(fields);

    return String.format("CREATE TABLE IF NOT EXISTS %s %s", tableName, fields);
  }

  public static String countTable(String tableName) {
    assertNotNull(tableName);
    return String.format("SELECT count(*) from %s", tableName);
  }
}

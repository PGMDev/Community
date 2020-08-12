package dev.pgm.community.database.query;

import static com.google.common.base.Preconditions.checkNotNull;

import tc.oc.pgm.util.concurrent.ThreadSafeConnection.Query;

/** TableQuery used for creating new SQL tables * */
public class TableQuery implements Query {

  private final String tableName;
  private final String fields;

  public TableQuery(String tableName, String fields) {
    this.tableName = checkNotNull(tableName);
    this.fields = checkNotNull(fields);
  }

  @Override
  public String getFormat() {
    return String.format("CREATE TABLE IF NOT EXISTS %s %s", tableName, fields);
  }
}

package dev.pgm.community.feature;

import co.aikar.idb.DB;
import dev.pgm.community.database.Query;
import java.util.concurrent.CompletableFuture;

/** Base implementation of {@link SQLFeature} * */
public abstract class SQLFeatureBase<T, R> implements SQLFeature<T, R> {

  private final String tableName;
  private final String fields;

  public SQLFeatureBase(String tableName, String fields) {
    this.tableName = tableName;
    this.fields = fields;
    createTable();
  }

  @Override
  public void createTable() {
    DB.executeUpdateAsync(Query.createTable(tableName, fields));
  }

  public CompletableFuture<Integer> count() {
    return DB.getFirstColumnAsync(Query.countTable(tableName));
  }
}

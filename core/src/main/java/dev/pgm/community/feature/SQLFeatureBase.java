package dev.pgm.community.feature;

import dev.pgm.community.database.DatabaseExecutor;
import dev.pgm.community.database.Query;
import java.util.concurrent.CompletableFuture;

/** Base implementation of {@link SQLFeature} * */
public abstract class SQLFeatureBase<T, R> implements SQLFeature<T, R> {

  private final String tableName;
  private final String fields;

  private CompletableFuture<Integer> tableReady = CompletableFuture.completedFuture(0);

  public SQLFeatureBase(String tableName, String fields) {
    this.tableName = tableName;
    this.fields = fields;
    createTable();
  }

  @Override
  public void createTable() {
    this.tableReady = DatabaseExecutor.executeUpdateAsync(Query.createTable(tableName, fields));
  }

  protected CompletableFuture<Integer> getTableReady() {
    return tableReady;
  }

  public CompletableFuture<Integer> count() {
    return DatabaseExecutor.queryFirstAsync(Query.countTable(tableName), result -> result.getInt(1))
        .thenApplyAsync(result -> result == null ? 0 : result);
  }
}

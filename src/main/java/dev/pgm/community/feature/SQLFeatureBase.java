package dev.pgm.community.feature;

import dev.pgm.community.database.DatabaseConnection;
import dev.pgm.community.database.query.CountQuery;
import dev.pgm.community.database.query.TableQuery;
import java.util.concurrent.CompletableFuture;

/** Base implementation of {@link SQLFeature} * */
public abstract class SQLFeatureBase<T> implements SQLFeature<T> {

  private final String tableName;
  private final String fields;
  private final DatabaseConnection database;

  public SQLFeatureBase(DatabaseConnection database, String tableName, String fields) {
    this.database = database;
    this.tableName = tableName;
    this.fields = fields;
    createTable();
  }

  @Override
  public void createTable() {
    database.submitQuery(new TableQuery(tableName, fields));
  }

  public CompletableFuture<Integer> count() {
    return getDatabase()
        .submitQueryComplete(new CountQuery(tableName))
        .thenApplyAsync(query -> CountQuery.class.cast(query).getCount());
  }

  @Override
  public DatabaseConnection getDatabase() {
    return database;
  }
}

package dev.pgm.community.feature;

import dev.pgm.community.database.DatabaseConnection;
import dev.pgm.community.database.query.TableQuery;

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

  @Override
  public DatabaseConnection getDatabase() {
    return database;
  }
}

package dev.pgm.community.feature;

import dev.pgm.community.database.DatabaseConnection;
import dev.pgm.community.database.Savable;

public interface SQLFeature<T, R> extends Savable<T, R> {

  /** Create the SQL table for a data set */
  void createTable();

  /**
   * Get the existing database connection
   *
   * @return a DatabaseConnection
   */
  DatabaseConnection getDatabase();
}

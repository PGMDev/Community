package dev.pgm.community.database;

import java.sql.SQLException;
import tc.oc.pgm.util.concurrent.ThreadSafeConnection;
import tc.oc.pgm.util.text.TextParser;

public class DatabaseConnection extends ThreadSafeConnection {

  public DatabaseConnection(String uri, int maxConnections) throws SQLException {
    super(() -> TextParser.parseSqlConnection(uri), maxConnections);
  }
}

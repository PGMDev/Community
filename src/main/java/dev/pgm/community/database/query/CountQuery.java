package dev.pgm.community.database.query;

import static com.google.common.base.Preconditions.checkNotNull;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import tc.oc.pgm.util.concurrent.ThreadSafeConnection.Query;

public class CountQuery implements Query {

  private String tableName;
  private int count = 0;

  public CountQuery(String tableName) {
    this.tableName = checkNotNull(tableName);
  }

  public int getCount() {
    return count;
  }

  @Override
  public String getFormat() {
    return String.format("SELECT count(*) from %s", tableName);
  }

  @Override
  public void query(PreparedStatement statement) throws SQLException {
    try (final ResultSet result = statement.executeQuery()) {
      if (result.next()) {
        count = result.getInt(1);
      }
    }
  }
}

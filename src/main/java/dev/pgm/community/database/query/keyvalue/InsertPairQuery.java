package dev.pgm.community.database.query.keyvalue;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import tc.oc.pgm.util.concurrent.ThreadSafeConnection.Query;

public class InsertPairQuery implements Query {

  private String keyField;
  private String valueField;
  private String key;
  private String value;
  private String table;

  public InsertPairQuery(
      String keyField, String valueField, String key, String value, String table) {
    this.keyField = keyField;
    this.valueField = valueField;
    this.key = key;
    this.value = value;
    this.table = table;
  }

  @Override
  public String getFormat() {
    return "INSERT INTO " + table + " (" + keyField + "," + valueField + ") VALUES (?,?)";
  }

  @Override
  public void query(PreparedStatement statement) throws SQLException {
    statement.setString(1, key);
    statement.setString(2, value);
    statement.execute();
  }
}

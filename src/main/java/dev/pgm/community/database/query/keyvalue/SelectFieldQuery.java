package dev.pgm.community.database.query.keyvalue;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import tc.oc.pgm.util.concurrent.ThreadSafeConnection.Query;

public class SelectFieldQuery implements Query {

  private final String table;
  private final String keyName;
  private final String keyValue;
  private final String targetField;
  private KeyValuePair pair;

  public SelectFieldQuery(String keyName, String keyValue, String targetField, String table) {
    this.keyName = keyName;
    this.keyValue = keyValue;
    this.targetField = targetField;
    this.table = table;
  }

  @Override
  public String getFormat() {
    return "SELECT " + targetField + " FROM " + table + " WHERE " + keyName + " = ?";
  }

  public KeyValuePair getPair() {
    return pair;
  }

  @Override
  public void query(PreparedStatement statement) throws SQLException {
    if (pair != null) return;
    statement.setString(1, keyValue);
    try (final ResultSet result = statement.executeQuery()) {
      if (!result.next()) {
        return;
      }
      String value = result.getString(targetField);
      this.pair = new KeyValuePairImpl(keyValue, value);
    }
  }
}

package dev.pgm.community.utils;

import co.aikar.idb.DbRow;

public class DatabaseUtils {

  public static boolean parseBoolean(DbRow row, String fieldName) throws ClassCastException {
    Object obj = row.get(fieldName);
    if (obj instanceof Integer) {
      int activeInt = (Integer) obj;
      return (activeInt != 0);
    } else if (obj instanceof Boolean) {
      return (Boolean) obj;
    } else {
      throw new ClassCastException(
          "Unexpected type for '" + fieldName + "': " + obj.getClass().getName());
    }
  }

  public static long parseLong(DbRow row, String fieldName) throws ClassCastException {
    Object obj = row.get(fieldName);
    if (obj instanceof String) {
      String rawLong = (String) obj;
      return Long.parseLong(rawLong);
    } else if (obj instanceof Long) {
      return (Long) obj;
    } else if (obj instanceof Integer) {
      return (Integer) obj;
    } else {
      throw new ClassCastException(
          "Unexpected type for '" + fieldName + "': " + obj.getClass().getName());
    }
  }
}

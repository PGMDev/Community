package dev.pgm.community.users.services;

public interface AddressQuery {

  String IP_ADDRESS_FIELD = "address";
  String IP_ID_FIELD = "ip_id";
  String USER_ID_FIELD = "user_id";
  String DATE_FIELD = "last_time";

  String IP_TABLE_FIELDS =
      String.format("(%s VARCHAR(15), %s VARCHAR(36))", IP_ADDRESS_FIELD, IP_ID_FIELD);
  String IP_TABLE_NAME = "addresses";

  String IP_USER_TABLE_FIELDS =
      String.format("(%s VARCHAR(36), %s VARCHAR(36))", USER_ID_FIELD, IP_ID_FIELD);
  String IP_USER_TABLE_NAME = "ip_history";

  String LATEST_IP_TABLE_FIELDS = String.format(
      "(%s VARCHAR(36) PRIMARY KEY, %s VARCHAR(15), %s LONG)",
      USER_ID_FIELD, IP_ADDRESS_FIELD, DATE_FIELD);
  String LATEST_IP_TABLE_NAME = "latest_ip";

  String INSERT_LATEST_IP_QUERY = "REPLACE INTO "
      + LATEST_IP_TABLE_NAME
      + "("
      + USER_ID_FIELD
      + ","
      + IP_ADDRESS_FIELD
      + ","
      + DATE_FIELD
      + ")"
      + " VALUES(?,?,?)";

  String SELECT_IP_QUERY =
      "SELECT " + IP_ID_FIELD + " FROM " + IP_TABLE_NAME + " WHERE " + IP_ADDRESS_FIELD + " = ?";

  String INSERT_IP_QUERY = "INSERT INTO "
      + IP_TABLE_NAME
      + " ("
      + IP_ADDRESS_FIELD
      + ","
      + IP_ID_FIELD
      + ") VALUES (?,?)";

  String INSERT_IP_USER_QUERY = "INSERT INTO "
      + IP_USER_TABLE_NAME
      + " ("
      + USER_ID_FIELD
      + ","
      + IP_ID_FIELD
      + ") VALUES (?,?)";

  String SELECT_LATEST_IP_QUERY =
      "SELECT * FROM " + LATEST_IP_TABLE_NAME + " WHERE " + USER_ID_FIELD + " = ?";

  String SELECT_IP_HISTORY_QUERY = "SELECT ip_id FROM " + IP_USER_TABLE_NAME + " WHERE user_id = ?";

  String SELECT_IP_ID_QUERY =
      "SELECT " + IP_ADDRESS_FIELD + " FROM " + IP_TABLE_NAME + " WHERE " + IP_ID_FIELD + " = ?";

  String SELECT_ALTS_QUERY =
      "SELECT " + USER_ID_FIELD + " FROM " + IP_USER_TABLE_NAME + " WHERE " + IP_ID_FIELD + " = ?";
}

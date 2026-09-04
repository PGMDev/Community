package dev.pgm.community.settings.services;

public interface UserSettingsQuery {

  String TABLE_NAME = "user_settings";
  String TABLE_FIELDS = "(id VARCHAR(36), "
      + "setting VARCHAR(32), "
      + "setting_value VARCHAR(32), "
      + "PRIMARY KEY (id, setting))";

  String SELECT_SETTINGS_QUERY = "SELECT * from " + TABLE_NAME + " where id = ?";
}

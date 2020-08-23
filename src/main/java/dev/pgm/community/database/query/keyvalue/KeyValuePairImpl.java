package dev.pgm.community.database.query.keyvalue;

public class KeyValuePairImpl implements KeyValuePair {

  private String key;
  private String value;

  public KeyValuePairImpl(String key, String value) {
    this.key = key;
    this.value = value;
  }

  @Override
  public String getKey() {
    return key;
  }

  @Override
  public String getValue() {
    return value;
  }
}

package dev.pgm.community.utils;

import java.util.UUID;
import java.util.regex.Pattern;

public class NameUtils {

  static final Pattern NAME_REGEX = Pattern.compile("[a-zA-Z0-9_]{1,16}");

  public static boolean isMinecraftName(String name) {
    return NAME_REGEX.matcher(name).matches();
  }

  public static boolean isPlayerId(String uuid) {
    try {
      UUID playerId = UUID.fromString(uuid);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  public static boolean isIdentifier(String input) {
    return isMinecraftName(input) || isPlayerId(input);
  }
}

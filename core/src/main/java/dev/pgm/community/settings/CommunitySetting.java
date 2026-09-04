package dev.pgm.community.settings;

import org.jspecify.annotations.Nullable;

/**
 * Registry of all per-player settings.
 *
 * <p>Settings are stored as generic key/value strings (see
 * {@link dev.pgm.community.settings.services.UserSettingsQuery}), so adding a new setting only
 * requires a new entry here plus the code that consumes it. Only non-default values are persisted;
 * a missing row resolves to {@link #getDefaultValue()}.
 *
 * <p>Keys are stored in the database and must never be renamed once released!
 */
public enum CommunitySetting {
  FRIEND_REQUESTS(
      "friend_requests",
      "true",
      "Friend Requests",
      "Whether other players can send you friend requests"),
  SQUAD_INVITES(
      "squad_invites", "true", "Party Invites", "Whether other players can invite you to a party");

  private final String key;
  private final String defaultValue;
  private final String displayName;
  private final String description;

  CommunitySetting(String key, String defaultValue, String displayName, String description) {
    this.key = key;
    this.defaultValue = defaultValue;
    this.displayName = displayName;
    this.description = description;
  }

  public String getKey() {
    return key;
  }

  public String getDefaultValue() {
    return defaultValue;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getDescription() {
    return description;
  }

  @Nullable
  public static CommunitySetting fromKey(String key) {
    for (CommunitySetting setting : values()) {
      if (setting.getKey().equalsIgnoreCase(key)) {
        return setting;
      }
    }
    return null;
  }
}

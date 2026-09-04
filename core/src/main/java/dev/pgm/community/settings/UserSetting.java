package dev.pgm.community.settings;

import java.util.UUID;

public record UserSetting(UUID playerId, String key, String value) {}

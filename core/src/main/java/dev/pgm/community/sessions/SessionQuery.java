package dev.pgm.community.sessions;

import java.util.UUID;

/**
 * @param playerId the UUID of the player
 * @param ignoreDisguised whether to skip disguised sessions and return the latest public one
 */
public record SessionQuery(UUID playerId, boolean ignoreDisguised) {}

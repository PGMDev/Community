package dev.pgm.community.serverlinks.types;

import org.jspecify.annotations.NullMarked;

/**
 * Represents a built-in server link type that will be auto-translated by the Minecraft client and
 * possibly have special functionality. Keep in sync with Paper's org.bukkit.ServerLinks.Type.
 */
@NullMarked
public enum ServerLinkBuiltinType {
  REPORT_BUG,
  COMMUNITY_GUIDELINES,
  SUPPORT,
  STATUS,
  FEEDBACK,
  COMMUNITY,
  WEBSITE,
  FORUMS,
  NEWS,
  ANNOUNCEMENTS;
}

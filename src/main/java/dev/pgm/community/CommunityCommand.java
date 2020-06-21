package dev.pgm.community;

import org.bukkit.ChatColor;

public interface CommunityCommand {

  default String format(String format, Object... args) {
    return String.format(
        ChatColor.translateAlternateColorCodes('&', format != null ? format : ""), args);
  }
}

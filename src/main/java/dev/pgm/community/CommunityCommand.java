package dev.pgm.community;

import co.aikar.commands.BaseCommand;
import org.bukkit.ChatColor;

public abstract class CommunityCommand extends BaseCommand {

  // Used to quickly format messages while in dev, move all final messages to TextComponents
  protected String format(String format, Object... args) {
    return String.format(
        ChatColor.translateAlternateColorCodes('&', format != null ? format : ""), args);
  }
}

package dev.pgm.community;

public interface CommunityPermissions {

  // TODO: Copy PGM format and style, register groups of permission nodes like moderator/admin/dev
  // etc

  // Root permission node
  String ROOT = "community";

  // Moderation:

  // Punishment Types
  String KICK = ROOT + ".kick"; // Access to the /kick command
  String WARN = ROOT + ".warn"; // Access to the /warn command
  String MUTE = ROOT + ".mute"; // Access to the /mute command
  String BAN = ROOT + ".ban"; // Access to the /ban command

  // Punishment related commands
  String LOOKUP = ROOT + ".lookup"; // Access to the /lookup command
  String UNBAN = ROOT + ".pardon"; // Access to the /unban command
  String PUNISH = ROOT + ".punish"; // Access to punishment commands (/rp, /ph)

  // Reports
  String REPORTS = ROOT + ".reports"; // Access to view report broadcast & report history

  // Staff
  String RELOAD = ROOT + ".reload";
  String RESTRICTED = ROOT + ".restricted"; // Access to restricted info (e.g IP addresses)

  // Teleports
  String TELEPORT = ROOT + ".teleport"; // Access to teleport to another player
  String TELEPORT_OTHERS = TELEPORT + ".others"; // Access to teleport other players
  String TELEPORT_LOCATION =
      TELEPORT + ".location"; // Access to teleport to a world location (coords)
  String TELEPORT_ALL = TELEPORT + ".all"; // Access to teleport everyone

  // Chat Management
  String CHAT_MANAGEMENT =
      ROOT + ".chat"; // Able to use /chat commands, and exempt during lock & slowmode

  // Friends
  String FRIENDSHIP = ROOT + ".friendship"; // Access to /friend commands

  // General Staff
  String STAFF =
      ROOT + ".staff"; // Receive staff broadcasts and see disguised players (maybe add a different
  // node later)

  // Freeze
  String FREEZE = ROOT + ".freeze";

  // Commands
  String FLIGHT = ROOT + ".fly";
  String FLIGHT_SPEED = FLIGHT + ".speed";

  // TODO Setup different groups like moderation

}

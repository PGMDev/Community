package dev.pgm.community;

public interface CommunityPermissions {

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

  // TODO Setup different groups like moderation

}

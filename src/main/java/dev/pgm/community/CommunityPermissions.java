package dev.pgm.community;

public interface CommunityPermissions {

  // Root permission node
  String ROOT = "community";

  String KICK = ROOT + ".kick"; // Access to the /kick command
  String WARN = ROOT + ".warn"; // Access to the /warn command
  String MUTE = ROOT + ".mute"; // Access to the /mute command
  String BAN = ROOT + ".ban"; // Access to the /ban command

  String UNBAN = ROOT + ".pardon"; // Access to the /unban command
  String LOOKUP = ROOT + ".lookup"; // Access to the /lookup command

  String PUNISH = ROOT + ".punish"; // Access to punishment commands (/rp, /ph)
}

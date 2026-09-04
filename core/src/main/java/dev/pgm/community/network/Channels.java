package dev.pgm.community.network;

/** Channels - Names of Channels used by NetworkUpdates * */
public class Channels {

  // PUNISHMENTS - Used to alert when a new punishment was issued for a player
  public static final String PUNISHMENTS = formatChannel("punishments");

  // ASSISTANCE - Used to broadcast reports/assist requests to other servers
  public static final String ASSISTANCE = formatChannel("assistance");

  // CHAT - Used to broadcast chat messages to other servers
  public static final String CHAT = formatChannel("chat");

  // PUNISHMENT_UPDATE - Used to alert servers of unmuted or unbanned players
  public static final String PUNISHMENT_UPDATE = formatChannel("punishment_update");

  // FRIENDSHIP_UPDATE - Used to update a player's cached friendships on other servers
  public static final String FRIENDSHIP_UPDATE = formatChannel("friendship_update");

  // SETTING_UPDATE - Used to update a player's cached settings on other servers
  public static final String SETTING_UPDATE = formatChannel("setting_update");

  private static String formatChannel(String name) {
    return "community_" + name;
  }
}

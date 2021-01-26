package dev.pgm.community.network;

/** Channels - Names of Channels used by NetworkUpdates * */
public class Channels {

  // PUNISHMENTS - Used to alert which a new punishment was issued for a player
  public static final String PUNISHMENTS = formatChannel("punishments");

  private static final String formatChannel(String name) {
    return "community:" + name;
  }
}

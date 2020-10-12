package dev.pgm.community.utils;

import tc.oc.pgm.util.chat.Sound;

public class Sounds {

  // Played when a player is reported
  public static final Sound PLAYER_REPORT = new Sound("random.pop", 1f, 1.2f);

  // Played when a ban evader joins the server
  public static final Sound BAN_EVASION = new Sound("random.pop", 1f, 0.8f);

  // Played when a player is warned
  public static final Sound WARN_SOUND = new Sound("mob.enderdragon.growl", 1f, 1f);

  // Played when an infraction is lifted
  public static final Sound PUNISHMENT_PARDON = new Sound("note.harp", 1f, 1.5f);

  // Played when a punishment is issued
  public static final Sound PUNISHMENT_ISSUE = new Sound("item.fireCharge.use", 1f, 0.3f);

  // Played when a player is teleported via command
  public static final Sound TELEPORT = new Sound("mob.endermen.portal", 1f, 0.9f);

  // Played when player logs in and has a pending friend request
  public static final Sound FRIEND_REQUEST_LOGIN = new Sound("note.harp", 1f, 1.2f);
}

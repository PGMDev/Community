package dev.pgm.community.utils;

import tc.oc.pgm.util.chat.Sound;

public class Sounds {

  // Played when a player is reported
  public static final Sound PLAYER_REPORT = new Sound("random.pop", 1f, 1.2f);

  // Played when a ban evader joins the server
  public static final Sound BAN_EVASION = new Sound("random.pop", 1f, 0.8f);

  // Played when an infraction is lifted
  public static final Sound PUNISHMENT_PARDON = new Sound("note.harp", 1f, 1.5f);

  // Played when a punishment is issued
  public static final Sound PUNISHMENT_ISSUE = new Sound("item.fireCharge.use", 1f, 0.3f);
}

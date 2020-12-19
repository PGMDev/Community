package dev.pgm.community.utils;

import static net.kyori.adventure.key.Key.key;
import static net.kyori.adventure.sound.Sound.sound;

import net.kyori.adventure.sound.Sound;

public class Sounds {

  // Played when a player is reported
  public static final Sound PLAYER_REPORT = sound(key("random.pop"), Sound.Source.MASTER, 1f, 1.2f);

  // Played when a ban evader joins the server
  public static final Sound BAN_EVASION = sound(key("random.pop"), Sound.Source.MASTER, 1f, 0.8f);

  // Played when a player is warned
  public static final Sound WARN_SOUND =
      sound(key("mob.enderdragon.growl"), Sound.Source.MASTER, 1f, 1f);

  // Played when an infraction is lifted
  public static final Sound PUNISHMENT_PARDON =
      sound(key("note.harp"), Sound.Source.MASTER, 0.7f, 1.5f);

  // Played when a player is teleported via command
  public static final Sound TELEPORT =
      sound(key("mob.endermen.portal"), Sound.Source.MASTER, 1f, 0.9f);

  // Played when player logs in and has a pending friend request
  public static final Sound FRIEND_REQUEST_LOGIN =
      sound(key("note.harp"), Sound.Source.MASTER, 1f, 1.2f);
}

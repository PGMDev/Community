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
      sound(key("mob.endermen.portal"), Sound.Source.MASTER, 0.7f, 0.9f);

  // Played when player logs in and has a pending friend request
  public static final Sound FRIEND_REQUEST_LOGIN =
      sound(key("note.harp"), Sound.Source.MASTER, 1f, 1.2f);

  // Played when a player requests help
  public static final Sound HELP_REQUEST =
      sound(key("mob.cat.meow"), Sound.Source.MASTER, 1f, 0.9f);

  // Played when a message is broadcasted (/broadcast)
  public static final Sound BROADCAST = sound(key("note.pling"), Sound.Source.MASTER, 1f, 1.4f);

  // Played when a message is sent to admin chat
  public static final Sound ADMIN_CHAT = sound(key("random.orb"), Sound.Source.MASTER, 1f, 0.7f);

  // Played when a a prominent message is broadcasted
  public static final Sound ALERT = sound(key("note.harp"), Sound.Source.MASTER, 1f, 0.1f);

  // Played when a target is selected for the teleport hook
  public static final Sound TARGET_CONFIRM =
      sound(key("random.wood_click"), Sound.Source.MASTER, 1f, 1.5f);

  // Played when a player receives tokens (login or /token give)
  public static final Sound GET_TOKENS =
      sound(key("random.levelup"), Sound.Source.MASTER, 1f, 1.1f);

  // Played when a player spends tokens (/sponsor)
  public static final Sound SPEND_TOKENS =
      sound(key("random.anvil_land"), Sound.Source.MASTER, 1f, 1.3f);
}

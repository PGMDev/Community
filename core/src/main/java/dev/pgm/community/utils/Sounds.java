package dev.pgm.community.utils;

import static tc.oc.pgm.util.bukkit.Sounds.sound;

import net.kyori.adventure.sound.Sound;

public class Sounds {

  // Played when a player is reported
  public static final Sound PLAYER_REPORT = sound("ITEM_PICKUP", "ENTITY_ITEM_PICKUP", 1f, 1.2f);

  // Played when a ban evader joins the server
  public static final Sound BAN_EVASION = sound("ITEM_PICKUP", "ENTITY_ITEM_PICKUP", 1f, 0.8f);

  // Played when a player is warned
  public static final Sound WARN_SOUND = sound("ENDERDRAGON_GROWL", "ENTITY_ENDER_DRAGON_GROWL");

  // Played when a player is frozen
  public static final Sound FREEZE_SOUND = WARN_SOUND;

  // Played when a player is unfrozen
  public static final Sound THAW_SOUND =
      sound("ENDERDRAGON_GROWL", "ENTITY_ENDER_DRAGON_GROWL", 1f, 2f);

  // Played when an infraction is lifted
  public static final Sound PUNISHMENT_PARDON =
      sound("NOTE_PIANO", "BLOCK_NOTE_BLOCK_HARP", 0.7f, 1.5f);

  // Played when a player is teleported via command
  public static final Sound TELEPORT =
      sound("ENDERMAN_TELEPORT", "ENTITY_ENDERMAN_TELEPORT", 0.7f, 0.9f);

  // Played when player logs in and has a pending friend request
  public static final Sound FRIEND_REQUEST_LOGIN =
      sound("NOTE_PIANO", "BLOCK_NOTE_BLOCK_HARP", 1f, 1.2f);

  // Played when a player requests help
  public static final Sound HELP_REQUEST = sound("CAT_MEOW", "ENTITY_CAT_AMBIENT", 1f, 0.9f);

  // Played when a message is broadcasted (/broadcast)
  public static final Sound BROADCAST = sound("NOTE_PLING", "BLOCK_NOTE_BLOCK_PLING", 1f, 1.4f);

  // Played when a message is sent to admin chat
  public static final Sound ADMIN_CHAT =
      sound("ORB_PICKUP", "ENTITY_EXPERIENCE_ORB_PICKUP", 1f, 0.7f);

  // Played when a a prominent message is broadcasted
  public static final Sound ALERT = sound("NOTE_PIANO", "BLOCK_NOTE_BLOCK_HARP", 1f, 0.1f);

  // Played when a target is selected for the teleport hook
  public static final Sound TARGET_CONFIRM =
      sound("WOOD_CLICK", "BLOCK_WOODEN_BUTTON_CLICK_ON", 1f, 1.5f);

  // Played when a player receives tokens (login or /token give)
  public static final Sound GET_TOKENS = sound("LEVEL_UP", "ENTITY_PLAYER_LEVELUP", 1f, 1.1f);

  // Played when a player spends tokens (/sponsor)
  public static final Sound SPEND_TOKENS = sound("ANVIL_LAND", "BLOCK_ANVIL_LAND", 1f, 1.3f);
}

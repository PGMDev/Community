package dev.pgm.community.moderation.punishments;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.translatable;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/** Types of Punishments* */
public enum PunishmentType {
  MUTE(false, true),
  WARN(false, false),
  KICK(true, false),
  BAN(true, true),
  TEMP_BAN(true, true);

  private String PREFIX_TRANSLATE_KEY = "moderation.type.";
  private String SCREEN_TRANSLATE_KEY = "moderation.screen.";

  private final boolean screen;
  private final boolean canRescind;

  /**
   * A Punishment Type
   *
   * @param screen Whether punishment has a custom kick screen
   * @param rescind Whether punishment can be revoked
   */
  PunishmentType(boolean screen, boolean rescind) {
    this.screen = screen;
    this.canRescind = rescind;
  }

  /**
   * Whether this punishment can be revoked manually (mutes/ban)
   *
   * @return if punishment can be revoked
   */
  public boolean canRescind() {
    return canRescind;
  }

  public Component getPunishmentPrefix() {
    return translatable(PREFIX_TRANSLATE_KEY + name().toLowerCase(), NamedTextColor.GOLD);
  }

  public Component getPunishmentPrefix(Component time) {
    return translatable(PREFIX_TRANSLATE_KEY + name().toLowerCase(), NamedTextColor.GOLD, time);
  }

  public Component getScreenComponent(Component reason) {
    if (!screen) return empty();
    return translatable(SCREEN_TRANSLATE_KEY + name().toLowerCase(), NamedTextColor.GOLD, reason);
  }

  public static boolean isBan(PunishmentType type) {
    return type.equals(BAN) || type.equals(TEMP_BAN);
  }
}

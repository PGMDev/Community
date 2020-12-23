package dev.pgm.community.mutations;

import static net.kyori.adventure.text.Component.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

/** Mutation - Represents a set of features that can be enabled/disabled during a PGM match * */
public interface Mutation {

  /** Called when a mutation is enabled */
  void enable();

  /** Called when a mutation is disabled */
  void disable();

  /**
   * Get whether this mutation has been enabled
   *
   * @return true if enabled, false if not
   */
  boolean isEnabled();

  /**
   * Gets the {@link MutationType} of this mutation
   *
   * @return the mutation type
   */
  MutationType getType();

  /**
   * Gets whether it is safe to enable current mutation Ex. If map is rage, don't allow rage
   * mutation to be enabled
   *
   * @return true if safe to enable, false if not
   */
  boolean canEnable();

  default Component getName() {
    return text(
            getType().getDisplayName(),
            isEnabled() ? NamedTextColor.GREEN : NamedTextColor.RED,
            TextDecoration.BOLD)
        .hoverEvent(HoverEvent.showText(text(getType().getDescription(), NamedTextColor.GRAY)));
  }
}

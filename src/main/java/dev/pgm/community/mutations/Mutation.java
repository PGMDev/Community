package dev.pgm.community.mutations;

import static net.kyori.adventure.text.Component.text;

import dev.pgm.community.mutations.options.MutationOption;
import java.util.Set;
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
   * Gets a set of {@link MutationOption} for this mutation. Set will be empty if there are no
   * options.
   *
   * @return a set of {@link MutationOption}
   */
  Set<MutationOption> getOptions();

  /**
   * Gets whether it is safe to enable current mutation Ex. If map is rage, don't allow rage
   * mutation to be enabled
   *
   * @param existingMutations - A set of existing mutations to check
   * @return
   */
  boolean canEnable(Set<Mutation> existingMutations);

  default Component getName() {
    return text(
            getType().getDisplayName(),
            isEnabled() ? NamedTextColor.GREEN : NamedTextColor.RED,
            TextDecoration.BOLD)
        .hoverEvent(HoverEvent.showText(text(getType().getDescription(), NamedTextColor.GRAY)));
  }
}

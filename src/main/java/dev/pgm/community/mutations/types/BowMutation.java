package dev.pgm.community.mutations.types;

import dev.pgm.community.mutations.Mutation;
import java.util.Set;

// Denotes a mutation that modifies projectile type of a bow (only 1 per match)
public interface BowMutation {

  default boolean hasBowMutation(Set<Mutation> activeMutations) {
    return activeMutations.stream().anyMatch(m -> m instanceof BowMutation);
  }
}

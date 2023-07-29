package dev.pgm.community.mutations;

import com.google.common.collect.Sets;
import dev.pgm.community.Community;
import dev.pgm.community.mutations.options.MutationOption;
import java.util.Set;
import javax.annotation.Nullable;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import tc.oc.pgm.api.match.Match;

/** MutationBase - Foundation of all mutations */
public abstract class MutationBase implements Mutation, Listener {

  private boolean enabled;

  protected final Match match;
  private final MutationType type;

  public MutationBase(Match match, MutationType type) {
    this.match = match;
    this.type = type;
  }

  @Override
  public MutationType getType() {
    return type;
  }

  @Override
  public void enable() {
    if (!isEnabled()) {
      this.enabled = true;
      Community.get().getServer().getPluginManager().registerEvents(this, Community.get());
    }
  }

  @Override
  public void disable() {
    this.enabled = false;
    HandlerList.unregisterAll(this);
  }

  @Override
  public Set<MutationOption> getOptions() {
    return Sets.newHashSet();
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  protected boolean isParticipant(@Nullable Player player) {
    return player != null && match.getParticipant(player) != null;
  }
}

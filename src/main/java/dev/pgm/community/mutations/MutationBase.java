package dev.pgm.community.mutations;

import dev.pgm.community.Community;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import tc.oc.pgm.api.match.Match;

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
    if (!isEnabled() && canEnable()) {
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
  public boolean isEnabled() {
    return enabled;
  }
}

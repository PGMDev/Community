package dev.pgm.community.mutations.types;

import dev.pgm.community.Community;
import dev.pgm.community.mutations.MutationBase;
import dev.pgm.community.mutations.MutationType;
import java.util.Random;
import tc.oc.pgm.api.match.Match;

/** ScheduledMutationBase - A base for mutations which require a repeating task * */
public abstract class ScheduledMutationBase extends MutationBase {

  private int taskID;
  private final int seconds;

  protected final Random random;

  public ScheduledMutationBase(Match match, MutationType type, int seconds) {
    super(match, type);
    this.seconds = seconds;
    this.random = new Random();
  }

  public abstract void run();

  @Override
  public void enable() {
    super.enable();
    this.taskID =
        Community.get()
            .getServer()
            .getScheduler()
            .scheduleSyncRepeatingTask(Community.get(), this::run, 20L, 20L * seconds);
  }

  @Override
  public void disable() {
    super.disable();
    Community.get().getServer().getScheduler().cancelTask(taskID);
  }
}

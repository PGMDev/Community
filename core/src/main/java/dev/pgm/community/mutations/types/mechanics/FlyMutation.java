package dev.pgm.community.mutations.types.mechanics;

import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.title.Title.title;

import com.google.common.collect.Sets;
import dev.pgm.community.Community;
import dev.pgm.community.mutations.Mutation;
import dev.pgm.community.mutations.MutationType;
import dev.pgm.community.mutations.options.MutationOption;
import dev.pgm.community.mutations.options.MutationRangeOption;
import dev.pgm.community.mutations.types.KitMutationBase;
import dev.pgm.community.utils.BroadcastUtils;
import java.util.Collection;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title.Times;
import net.kyori.adventure.util.Ticks;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.kits.FlyKit;

/** FlyMutation - Enables the {@link FlyKit} for all players */
public class FlyMutation extends KitMutationBase {

  private static MutationRangeOption FLY_DISABLE_DELAY =
      new MutationRangeOption(
          "Flight Disable countdown",
          "Delay before flight is disabled",
          MutationType.FLY.getMaterial(),
          false,
          5,
          0,
          20);

  private static MutationRangeOption FLY_SPEED =
      new MutationRangeOption(
          "Flight Speed", "Speed of flight", MutationType.FLY.getMaterial(), true, 1, 1, 5);

  private int disableTaskID;
  private boolean disableTaskEnabled;

  public FlyMutation(Match match) {
    super(match, MutationType.FLY, getFlightKit());
  }

  @Override
  public Collection<MutationOption> getOptions() {
    return Sets.newHashSet(FLY_DISABLE_DELAY, FLY_SPEED);
  }

  @Override
  public void disable() {
    if (!match.isFinished()) {
      this.disableTaskID =
          Community.get()
              .getServer()
              .getScheduler()
              .scheduleSyncRepeatingTask(
                  Community.get(), new DisableFlightTask(FLY_DISABLE_DELAY.getValue()), 0L, 20L);
      this.disableTaskEnabled = true;
    } else {
      super.disable();
    }
  }

  @Override
  public boolean canEnable(Set<Mutation> existing) {
    return true;
  }

  private static FlyKit getFlightKit() {
    return new FlyKit(true, true, FLY_SPEED.getValue());
  }

  @EventHandler(priority = EventPriority.LOW)
  public void onMatchEnd(MatchFinishEvent event) {
    if (disableTaskEnabled) { // Special removal case due to new disable logic
      remove();
    }
  }

  private void remove() {
    Community.get().getServer().getScheduler().cancelTask(disableTaskID);
    super.disable();
  }

  private class DisableFlightTask implements Runnable {

    private int seconds;

    public DisableFlightTask(int seconds) {
      this.seconds = seconds;
    }

    @Override
    public void run() {
      if (seconds < 1) {
        remove();
      } else {
        Component time = text(seconds, NamedTextColor.RED, TextDecoration.BOLD);
        Component left =
            text()
                .append(text("second"))
                .append(text(seconds != 1 ? "s" : ""))
                .append(text(" left to land"))
                .color(NamedTextColor.GRAY)
                .build();
        BroadcastUtils.sendGlobalMessage(text().append(time).append(space()).append(left).build());
        match
            .getParticipants()
            .forEach(
                player ->
                    player.showTitle(
                        title(
                            time,
                            left,
                            Times.of(Ticks.duration(0), Ticks.duration(20), Ticks.duration(5)))));
        seconds--;
      }
    }
  }
}

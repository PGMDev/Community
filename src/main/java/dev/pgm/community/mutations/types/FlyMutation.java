package dev.pgm.community.mutations.types;

import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.title.Title.title;

import dev.pgm.community.Community;
import dev.pgm.community.mutations.MutationType;
import dev.pgm.community.utils.BroadcastUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title.Times;
import net.kyori.adventure.util.Ticks;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.kits.FlyKit;

public class FlyMutation extends KitMutation {

  private static final FlyKit KIT_ON = new FlyKit(true, true, 1);
  private static final FlyKit KIT_OFF = new FlyKit(false, false, 1);

  private static final int FLY_DISABLE_SECONDS = 5;

  private int disableTaskID;

  public FlyMutation(Match match) {
    super(match, MutationType.FLY, KIT_ON);
  }

  @Override
  public void disable() {
    super.disable();

    if (!match.isFinished()) {
      this.disableTaskID =
          Community.get()
              .getServer()
              .getScheduler()
              .scheduleSyncRepeatingTask(
                  Community.get(), new DisableFlightTask(FLY_DISABLE_SECONDS), 0L, 20L);
    }
    // TODO: add remove task to ask players to land
  }

  @Override
  public boolean canEnable() {
    return true;
  }

  private class DisableFlightTask implements Runnable {

    private int seconds;

    public DisableFlightTask(int seconds) {
      this.seconds = seconds;
    }

    @Override
    public void run() {
      if (seconds < 1) {
        giveAllKit(KIT_OFF);
        Community.get().getServer().getScheduler().cancelTask(disableTaskID);
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

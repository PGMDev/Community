package dev.pgm.community.mutations.types.world;

import com.google.common.collect.Sets;
import dev.pgm.community.Community;
import dev.pgm.community.mutations.Mutation;
import dev.pgm.community.mutations.MutationType;
import dev.pgm.community.mutations.types.ScheduledMutationBase;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.weather.WeatherChangeEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;

public class StormMutation extends ScheduledMutationBase {

  private static final int LIGHTNING_STRIKE_DELAY = 60;
  private static final int MAX_STRIKES = 5;

  public StormMutation(Match match) {
    super(match, MutationType.STORM, LIGHTNING_STRIKE_DELAY);
  }

  @Override
  public void enable() {
    super.enable();
    toggleStorm(true);
  }

  @Override
  public void disable() {
    toggleStorm(false);
    super.disable();
  }

  private void toggleStorm(boolean enabled) {
    World world = match.getWorld();
    int duration = enabled ? Integer.MAX_VALUE : 0;
    world.setStorm(enabled);
    world.setThundering(enabled);
    world.setWeatherDuration(duration);
    world.setThunderDuration(duration);
  }

  @Override
  public boolean canEnable(Set<Mutation> existing) {
    return true;
  }

  @Override
  public void run() {
    int strikes = match.getRandom().nextInt(MAX_STRIKES);
    Set<MatchPlayer> chosen = selectRandomPlayers(strikes);
    int delay = 0;
    for (MatchPlayer player : chosen) {
      delayedStrike(player, delay);
      delay += 2;
    }
  }

  private void delayedStrike(final MatchPlayer player, int delay) {
    Bukkit.getScheduler()
        .scheduleSyncDelayedTask(
            Community.get(),
            () -> {
              match.getWorld().spigot().strikeLightning(player.getLocation(), false);
            },
            20 * delay);
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onWeatherChange(WeatherChangeEvent event) {
    event.setCancelled(false);
  }

  private Set<MatchPlayer> selectRandomPlayers(int amount) {
    List<MatchPlayer> everyone = match.getParticipants().stream().collect(Collectors.toList());
    Set<MatchPlayer> chosen = Sets.newHashSet();
    int retrys = 0;
    while (chosen.size() < amount && retrys < 5) {
      MatchPlayer player = everyone.get(match.getRandom().nextInt(everyone.size()));
      if (player != null && !chosen.contains(player)) {
        chosen.add(player);
      } else {
        retrys++;
      }
    }
    return chosen;
  }
}

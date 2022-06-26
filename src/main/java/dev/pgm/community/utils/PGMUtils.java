package dev.pgm.community.utils;

import co.aikar.commands.InvalidCommandArgument;
import com.google.common.collect.Lists;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.blitz.BlitzMatchModule;
import tc.oc.pgm.rotation.MapPoolManager;
import tc.oc.pgm.rotation.pools.MapPool;

public class PGMUtils {

  public static final String SPACE = "\u2508";

  public static boolean isPGMEnabled() {
    Plugin pgmPlugin = Bukkit.getServer().getPluginManager().getPlugin("PGM");
    return pgmPlugin != null && pgmPlugin.isEnabled() && PGM.get() != null;
  }

  public static @Nullable Match getMatch() {
    return isPGMEnabled() && PGM.get().getMatchManager().getMatches().hasNext()
        ? PGM.get().getMatchManager().getMatches().next()
        : null;
  }

  public static List<String> convertMapNames(List<MapInfo> maps) {
    List<String> names = Lists.newArrayList();
    if (isPGMEnabled()) {
      names =
          maps.stream()
              .map(MapInfo::getName)
              .map(name -> name.replace(" ", SPACE))
              .collect(Collectors.toList());
    }
    return names;
  }

  public static List<String> getMapNames() {
    return convertMapNames(Lists.newArrayList(PGM.get().getMapLibrary().getMaps()));
  }

  public static List<String> getAllowedMapNames() {
    if (isPGMEnabled()) {
      return convertMapNames(
          Lists.newArrayList(PGM.get().getMapLibrary().getMaps()).stream()
              .filter(PGMUtils::isMapSizeAllowed)
              .collect(Collectors.toList()));
    }
    return Lists.newArrayList();
  }

  public static boolean compareMatchLength(Duration time) {
    if (isPGMEnabled()) {
      Match match = getMatch();
      return match.isRunning() && (match.getDuration().getSeconds() > time.getSeconds());
    }
    return true;
  }

  public static boolean isMatchRunning() {
    return isPGMEnabled() && getMatch().isRunning();
  }

  @Nullable
  public static MapInfo getCurrentMap() {
    return isPGMEnabled() && getMatch() != null ? getMatch().getMap() : null;
  }

  public static boolean isMapSizeAllowed(MapInfo map) {
    if (isPGMEnabled()) {
      Match match = getMatch();
      int participants = match.getParticipants().size();
      int observers = match.getObservers().size();
      int total = participants + (observers / 4);

      int max = map.getMaxPlayers().stream().reduce(0, Integer::sum);
      int lowerBound = participants;
      int upperBound = total + (int) (total * 0.35);

      return max >= lowerBound && max <= upperBound;
    }

    return true;
  }

  public static MapInfo parseMapText(String input) throws InvalidCommandArgument {
    if (input.contains(PGMUtils.SPACE)) {
      input = input.replaceAll(PGMUtils.SPACE, " ");
    }
    MapInfo map = PGM.get().getMapLibrary().getMap(input);

    if (map == null) {
      throw new InvalidCommandArgument(
          ChatColor.AQUA + input + ChatColor.RED + " is not a valid map name", false);
    }

    return map;
  }

  public static boolean isBlitz() {
    Match match = getMatch();
    if (match != null) {
      BlitzMatchModule bmm = match.getModule(BlitzMatchModule.class);
      return bmm != null;
    }
    return false;
  }

  public static void setMapPool(CommandSender sender, MapPool pool) {
    if (isPGMEnabled()) {
      if (PGM.get().getMapOrder() instanceof MapPoolManager) {
        MapPoolManager manager = (MapPoolManager) PGM.get().getMapOrder();
        manager.updateActiveMapPool(pool, getMatch(), true, sender, null, 0, true);
      }
    }
  }

  @Nullable
  public static MapPoolManager getMapPoolManager() {
    if (isPGMEnabled()) {
      if (PGM.get().getMapOrder() instanceof MapPoolManager) {
        return (MapPoolManager) PGM.get().getMapOrder();
      }
    }
    return null;
  }

  public static Optional<MapPool> getMapPool(String name) {
    if (isPGMEnabled()) {
      if (PGM.get().getMapOrder() instanceof MapPoolManager) {
        MapPoolManager manager = (MapPoolManager) PGM.get().getMapOrder();
        return Optional.ofNullable(manager.getMapPoolByName(name));
      }
    }
    return Optional.empty();
  }
}

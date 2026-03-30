package dev.pgm.community.utils;

import com.google.common.collect.Lists;
import dev.pgm.community.utils.compatibility.Materials;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapTag;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchPhase;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.blitz.BlitzMatchModule;
import tc.oc.pgm.rotation.MapPoolManager;
import tc.oc.pgm.rotation.pools.MapPool;
import tc.oc.pgm.util.text.TextException;

public class PGMUtils {

  public static final String SPACE = "\u2508";

  public static boolean isPGMEnabled() {
    Plugin pgmPlugin = Bukkit.getServer().getPluginManager().getPlugin("PGM");
    return pgmPlugin != null && pgmPlugin.isEnabled() && PGM.get() != null;
  }

  public static Match getMatch() {
    return isPGMEnabled() && PGM.get().getMatchManager().getMatches().hasNext()
        ? PGM.get().getMatchManager().getMatches().next()
        : null;
  }

  public static List<String> convertMapNames(List<MapInfo> maps) {
    List<String> names = Lists.newArrayList();
    if (isPGMEnabled()) {
      names = maps.stream()
          .map(MapInfo::getName)
          .map(name -> name.replace(" ", SPACE))
          .collect(Collectors.toList());
    }
    return names;
  }

  public static List<String> getMapNames() {
    return convertMapNames(Lists.newArrayList(PGM.get().getMapLibrary().getMaps()));
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

  public static MapInfo getCurrentMap() {
    return isPGMEnabled() && getMatch() != null ? getMatch().getMap() : null;
  }

  public record MapSizeBounds(int lowerBound, int upperBound) {}

  public static boolean isMapSizeAllowed(
      MapInfo map, int lowerBoundOffset, int upperBoundOffset, double scaleFactor) {
    if (isPGMEnabled()) {
      MapSizeBounds bounds = getMapSizeBounds(lowerBoundOffset, upperBoundOffset, scaleFactor);

      int max = getMapMaxSize(map);

      return max >= bounds.lowerBound() && max <= bounds.upperBound();
    }

    return true;
  }

  public static int getMapMaxSize(MapInfo map) {
    return map.getMaxPlayers().stream().reduce(0, Integer::sum);
  }

  public static MapSizeBounds getMapSizeBounds(
      int lowerBoundOffset, int upperBoundOffset, double scalingFactor) {
    if (!isPGMEnabled()) return new MapSizeBounds(0, 150);

    Match match = getMatch();
    boolean isFinished = match.getPhase() == MatchPhase.FINISHED;
    int participants = match.getParticipants().size();
    int observers = match.getObservers().size();
    int total = participants + (observers / 4);

    int lowerBound = participants;
    int upperBound = Math.max(5, total + (int) (total * scalingFactor));

    if (isFinished) {
      lowerBound = Math.max(0, lowerBound - lowerBoundOffset);
      upperBound += upperBoundOffset;
    }

    return new MapSizeBounds(lowerBound, upperBound);
  }

  public static MapInfo parseMapText(String input) throws TextException {
    if (input.contains(PGMUtils.SPACE)) {
      input = input.replace(PGMUtils.SPACE, " ");
    }
    MapInfo map = PGM.get().getMapLibrary().getMap(input);

    if (map == null) {
      throw TextException.exception(
          ChatColor.AQUA + input + ChatColor.RED + " is not a valid map name");
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
      if (PGM.get().getMapOrder() instanceof MapPoolManager manager) {
        manager.updateActiveMapPool(pool, getMatch(), true, sender, null, 0);
      }
    }
  }

  public static MapPoolManager getMapPoolManager() {
    if (isPGMEnabled()) {
      if (PGM.get().getMapOrder() instanceof MapPoolManager) {
        return (MapPoolManager) PGM.get().getMapOrder();
      }
    }
    return null;
  }

  public static MapPool getActiveMapPool() {
    MapPoolManager manager = getMapPoolManager();
    if (manager == null) return null;
    return manager.getActiveMapPool();
  }

  public static Optional<MapPool> getMapPool(String name) {
    if (isPGMEnabled()) {
      if (PGM.get().getMapOrder() instanceof MapPoolManager manager) {
        return Optional.ofNullable(manager.getMapPoolByName(name));
      }
    }
    return Optional.empty();
  }

  public static List<MatchPlayer> getPlayers(String permissionFilter) {
    if (!isPGMEnabled()) return Lists.newArrayList();
    return getMatch().getPlayers().stream()
        .filter(mp -> mp.getBukkit().hasPermission(permissionFilter))
        .collect(Collectors.toList());
  }

  public static Material mapTagMaterial(MapTag mapTag) {
    return switch (mapTag.getId()) {
      case "2teams" -> Material.LEATHER;
      case "ffa" -> Material.DIAMOND_SWORD;
      case "border" -> Materials.IRON_BARDING;
      case "wool" -> Materials.WOOL;
      case "controlpoint" -> Material.BEACON;
      case "flag" -> Materials.BANNER;
      case "classes" -> Material.FISHING_ROD;
      case "deathmatch" -> Material.STONE_SWORD;
      case "monument" -> Material.DIAMOND_PICKAXE;
      case "4teams" -> Materials.TRAP_DOOR;
      case "timelimit" -> Materials.WATCH;
      case "autotnt" -> Material.TNT;
      case "core" -> Material.OBSIDIAN;
      case "blitz" -> Material.EGG;
      case "scorebox" -> Materials.WEB;
      case "6teams" -> Materials.BED;
      case "rage" -> Material.BOW;
      case "3teams" -> Materials.WORKBENCH;
      case "terrain" -> Materials.GRASS;
      case "8teams" -> Materials.DYE;
      default -> Material.MAP;
    };
  }
}

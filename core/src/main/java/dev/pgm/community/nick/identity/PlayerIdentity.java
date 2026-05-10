package dev.pgm.community.nick.identity;

import static dev.pgm.community.util.PlayerUtils.PLAYER_UTILS;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.util.skin.Skin;

@NullMarked
public final class PlayerIdentity {
  public static final int MAX_NICK_LENGTH = 16;
  public static final PlayerIdentity PLAYER_IDENTITY = new PlayerIdentity();

  private final Map<UUID, Map<UUID, Skin>> playerSkins = new HashMap<>();
  private final Map<UUID, Map<UUID, String>> playerNames = new HashMap<>();
  private final Map<UUID, Map<UUID, String>> playerDisplayNames = new HashMap<>();
  private final Map<UUID, Map<String, HashSet<String>>> viewerTeamEntries = new HashMap<>();
  private final Map<UUID, Map<String, String>> viewerVisibleNames = new HashMap<>();

  private PlayerIdentity() {}

  public synchronized void set(
      Player player,
      Player viewer,
      @Nullable String displayName,
      @Nullable String nick,
      @Nullable Skin skin) {
    validateNick(nick);

    UUID playerId = player.getUniqueId();
    UUID viewerId = viewer.getUniqueId();

    set(playerSkins, playerId, viewerId, skin);
    set(playerNames, playerId, viewerId, nick);
    set(playerDisplayNames, playerId, viewerId, displayName);
    setVisibleName(viewerId, player.getName(), getName(player, viewer));
  }

  private static <T> void set(
      Map<UUID, Map<UUID, T>> identities, UUID playerId, UUID viewerId, @Nullable T value) {
    if (value == null) {
      Map<UUID, T> viewers = identities.get(playerId);
      if (viewers == null) return;

      viewers.remove(viewerId);
      if (viewers.isEmpty()) identities.remove(playerId);
      return;
    }

    identities.computeIfAbsent(playerId, k -> new HashMap<>()).put(viewerId, value);
  }

  public static void validateNick(@Nullable String name) {
    if (name != null && name.length() > MAX_NICK_LENGTH) {
      throw new IllegalArgumentException(
          "Player nick names are limited to " + MAX_NICK_LENGTH + " characters in length");
    }
  }

  public synchronized void clearPlayer(UUID playerId, String realName) {
    playerSkins.remove(playerId);
    playerNames.remove(playerId);
    playerDisplayNames.remove(playerId);
    viewerVisibleNames.values().removeIf(visibleNames -> {
      visibleNames.remove(realName);
      return visibleNames.isEmpty();
    });
  }

  public synchronized void clearViewer(UUID viewerId) {
    clearViewer(playerSkins, viewerId);
    clearViewer(playerNames, viewerId);
    clearViewer(playerDisplayNames, viewerId);
    viewerTeamEntries.remove(viewerId);
    viewerVisibleNames.remove(viewerId);
  }

  private static <T> void clearViewer(Map<UUID, Map<UUID, T>> identities, UUID viewerId) {
    identities.values().removeIf(viewers -> {
      viewers.remove(viewerId);
      return viewers.isEmpty();
    });
  }

  public synchronized void clearAll() {
    playerSkins.clear();
    playerNames.clear();
    playerDisplayNames.clear();
    viewerTeamEntries.clear();
    viewerVisibleNames.clear();
  }

  public synchronized @Nullable String getVisibleName(UUID viewerId, String realName) {
    Map<String, String> visibleNames = viewerVisibleNames.get(viewerId);
    return visibleNames == null ? null : visibleNames.get(realName);
  }

  private void setVisibleName(UUID viewerId, String realName, String visibleName) {
    if (realName.equals(visibleName)) {
      clearVisibleName(viewerId, realName);
      return;
    }

    viewerVisibleNames.computeIfAbsent(viewerId, k -> new HashMap<>()).put(realName, visibleName);
  }

  private void clearVisibleName(UUID viewerId, String realName) {
    Map<String, String> visibleNames = viewerVisibleNames.get(viewerId);
    if (visibleNames == null) return;

    visibleNames.remove(realName);
    if (visibleNames.isEmpty()) viewerVisibleNames.remove(viewerId);
  }

  public synchronized @Nullable String getTeamName(UUID viewerId, String entry) {
    Map<String, HashSet<String>> teamEntries = viewerTeamEntries.get(viewerId);
    if (teamEntries == null) return null;

    for (Map.Entry<String, HashSet<String>> team : teamEntries.entrySet()) {
      if (team.getValue().contains(entry)) return team.getKey();
    }

    return null;
  }

  public synchronized boolean hasTeamEntry(UUID viewerId, String teamName, String entry) {
    Map<String, HashSet<String>> teamEntries = viewerTeamEntries.get(viewerId);
    if (teamEntries == null) return false;

    HashSet<String> entries = teamEntries.get(teamName);
    return entries != null && entries.contains(entry);
  }

  public synchronized void createTeam(UUID viewerId, String teamName, Collection<String> entries) {
    removeTeam(viewerId, teamName);
    addTeamEntries(viewerId, teamName, entries);
  }

  public synchronized void removeTeam(UUID viewerId, String teamName) {
    Map<String, HashSet<String>> teamEntries = viewerTeamEntries.get(viewerId);
    if (teamEntries == null) return;

    teamEntries.remove(teamName);
    if (teamEntries.isEmpty()) viewerTeamEntries.remove(viewerId);
  }

  public synchronized void addTeamEntries(
      UUID viewerId, String teamName, Collection<String> entries) {
    if (entries.isEmpty()) return;

    Map<String, HashSet<String>> teamEntries =
        viewerTeamEntries.computeIfAbsent(viewerId, k -> new HashMap<>());
    teamEntries.values().forEach(team -> team.removeAll(entries));
    teamEntries.values().removeIf(Collection::isEmpty);
    teamEntries.computeIfAbsent(teamName, k -> new HashSet<>()).addAll(entries);
  }

  public synchronized void removeTeamEntries(
      UUID viewerId, String teamName, Collection<String> entries) {
    Map<String, HashSet<String>> teamEntries = viewerTeamEntries.get(viewerId);
    if (teamEntries == null) return;

    HashSet<String> team = teamEntries.get(teamName);
    if (team == null) return;

    team.removeAll(entries);
    if (team.isEmpty()) teamEntries.remove(teamName);
    if (teamEntries.isEmpty()) viewerTeamEntries.remove(viewerId);
  }

  public synchronized String getDisplayName(Player player, Player viewer) {
    String displayName = get(playerDisplayNames, player.getUniqueId(), viewer.getUniqueId());
    if (displayName != null) return displayName;

    return player.getDisplayName();
  }

  public synchronized boolean hasDisplayName(Player player, Player viewer) {
    return get(playerDisplayNames, player.getUniqueId(), viewer.getUniqueId()) != null;
  }

  public synchronized String getName(Player player, Player viewer) {
    String name = get(playerNames, player.getUniqueId(), viewer.getUniqueId());
    if (name != null) return name;

    return player.getName();
  }

  public synchronized boolean hasName(Player player, Player viewer) {
    return get(playerNames, player.getUniqueId(), viewer.getUniqueId()) != null;
  }

  public synchronized Skin getSkin(Player player, Player viewer) {
    Skin skin = get(playerSkins, player.getUniqueId(), viewer.getUniqueId());
    if (skin != null && !skin.isEmpty()) return skin;

    return PLAYER_UTILS.getPlayerSkin(player);
  }

  public synchronized boolean hasSkin(Player player, Player viewer) {
    Skin skin = get(playerSkins, player.getUniqueId(), viewer.getUniqueId());
    return skin != null && !skin.isEmpty();
  }

  private static <T> @Nullable T get(
      Map<UUID, Map<UUID, T>> identities, UUID playerId, UUID viewerId) {
    Map<UUID, T> viewers = identities.get(playerId);
    return viewers == null ? null : viewers.get(viewerId);
  }
}

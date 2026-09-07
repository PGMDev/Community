package dev.pgm.community.platform.modern;

import static dev.pgm.community.util.Supports.Variant.PAPER;

import dev.pgm.community.util.PlayerIdentities;
import dev.pgm.community.util.Supports;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.util.skin.Skin;

/**
 * Identity storage for {@link PacketManipulations}'s packet-based implementation of SportPaper's
 * per-viewer identity APIs
 *
 * <p>Changing an identity follows the same shape SportPaper uses natively: strip the player from
 * the viewer's client, mutate, then re-add. The player list entry has to be removed and re-added
 * because clients cache profiles by UUID and ignore a second ADD_PLAYER for a UUID they already
 * know, and the entity has to be re-tracked because its name and skin come from that entry.
 */
@NullMarked
@Supports(value = PAPER, minVersion = "1.21.11")
public class ModernPlayerIdentities implements PlayerIdentities {

  private final Map<UUID, Map<UUID, String>> names = new ConcurrentHashMap<>();
  private final Map<UUID, Map<UUID, String>> displayNames = new ConcurrentHashMap<>();
  private final Map<UUID, Map<UUID, Skin>> skins = new ConcurrentHashMap<>();
  private final Map<String, UUID> disguised = new ConcurrentHashMap<>();

  /**
   * Populated by {@code PacketManipulations}; we record what names players are sent for scoreboard
   * team updates, and which team their client filed them under. These need to match exactly to
   * avoid clientside protocol errors when names are swapped, but player list entries key by UUIDs,
   * so they're less fragile and accept profile changes more readily.
   */
  private final Map<UUID, Map<String, TeamEntry>> sentNames = new ConcurrentHashMap<>();

  /** A team entry as it exists on a viewer's client: which team, and under what name */
  private record TeamEntry(String team, String name) {}

  /**
   * The name to send when removing {@code entry} from {@code team}: {@code entry} itself when this
   * viewer was never sent anything else, or {@code null} when their client filed the entry under a
   * different team, in which case the removal has to be dropped rather than sent.
   */
  public @Nullable String removedName(UUID viewerId, String team, String entry) {
    TeamEntry tracked = get(sentNames, viewerId, entry);
    if (tracked == null) return entry;
    if (!tracked.team().equals(team)) return null;

    set(sentNames, viewerId, entry, null);
    return tracked.name();
  }

  /** Record what this viewer was sent in place of {@code entry}, and which team it went into */
  public void renamed(UUID viewerId, String team, String entry, String sent) {
    set(sentNames, viewerId, entry, entry.equals(sent) ? null : new TeamEntry(team, sent));
  }

  /** Forget what a viewer's client held in {@code team}; it drops those entries with the team */
  public void teamRemoved(UUID viewerId, String team) {
    Map<String, TeamEntry> tracked = sentNames.get(viewerId);
    if (tracked == null) return;

    tracked.values().removeIf(entry -> entry.team().equals(team));
    if (tracked.isEmpty()) sentNames.remove(viewerId, tracked);
  }

  public @Nullable String fakeName(UUID playerId, UUID viewerId) {
    return get(names, playerId, viewerId);
  }

  public @Nullable String fakeDisplayName(UUID playerId, UUID viewerId) {
    return get(displayNames, playerId, viewerId);
  }

  public @Nullable Skin fakeSkin(UUID playerId, UUID viewerId) {
    return get(skins, playerId, viewerId);
  }

  public @Nullable UUID disguisedId(String realName) {
    return disguised.get(realName);
  }

  @Override
  public void setIdentity(
      Player player,
      Player viewer,
      @Nullable String name,
      @Nullable String displayName,
      @Nullable Skin skin) {
    PlayerIdentities.validateName(name);
    if (skin != null && skin.isEmpty()) skin = null;

    UUID playerId = player.getUniqueId(), viewerId = viewer.getUniqueId();

    boolean profileChanged = !Objects.equals(fakeName(playerId, viewerId), name)
        || !Objects.equals(fakeSkin(playerId, viewerId), skin);
    boolean displayNameChanged = !Objects.equals(fakeDisplayName(playerId, viewerId), displayName);

    if (!profileChanged && !displayNameChanged) return;

    boolean refresh =
        !player.equals(viewer) && viewer.isOnline() && player.isOnline() && viewer.canSee(player);

    if (refresh && profileChanged) removeOnClient(player, viewer);

    set(names, playerId, viewerId, name);
    set(displayNames, playerId, viewerId, displayName);
    set(skins, playerId, viewerId, skin);
    reindex(player);

    if (!refresh) return;
    if (profileChanged) {
      addOnClient(player, viewer);
    } else {
      sendPlayerInfo(player, viewer);
    }
  }

  @Override
  public void clearIdentities(Player player) {
    for (UUID viewerId : viewersOf(player)) {
      Player viewer = Bukkit.getPlayer(viewerId);
      if (viewer != null) {
        clearIdentity(player, viewer);
      } else {
        forget(player.getUniqueId(), viewerId);
      }
    }
  }

  @Override
  public void forget(Player player) {
    UUID playerId = player.getUniqueId();
    for (var identities : List.of(names, displayNames, skins)) {
      identities.remove(playerId);
      identities.values().forEach(viewers -> viewers.remove(playerId));
    }
    disguised.remove(player.getName(), playerId);
    sentNames.remove(playerId);
    prune();
  }

  @Override
  public void clearAll() {
    for (Player player : Bukkit.getOnlinePlayers()) clearIdentities(player);
    names.clear();
    displayNames.clear();
    skins.clear();
    disguised.clear();
    sentNames.clear();
  }

  /** Strip the player from the viewer's client, so the next player list entry is not ignored */
  private void removeOnClient(Player player, Player viewer) {
    trackedEntity(player).ifPresent(tracked -> tracked.removePlayer(handle(viewer)));
    send(new ClientboundPlayerInfoRemovePacket(List.of(player.getUniqueId())), viewer);
    sendTeamEntry(player, viewer, ClientboundSetPlayerTeamPacket.Action.REMOVE);
  }

  /** Re-add the player to the viewer's client, picking up whichever identity is now stored */
  private void addOnClient(Player player, Player viewer) {
    sendTeamEntry(player, viewer, ClientboundSetPlayerTeamPacket.Action.ADD);
    sendPlayerInfo(player, viewer);
    trackedEntity(player).ifPresent(tracked -> tracked.updatePlayer(handle(viewer)));
  }

  private void sendPlayerInfo(Player player, Player viewer) {
    send(
        ClientboundPlayerInfoUpdatePacket.createSinglePlayerInitializing(
            handle(player), viewer.isListed(player)),
        viewer);
  }

  @SuppressWarnings("ConstantConditions")
  private void sendTeamEntry(
      Player player, Player viewer, ClientboundSetPlayerTeamPacket.Action action) {
    var team = viewer.getScoreboard().getEntryTeam(player.getName());
    if (team == null) return;

    send(
        ClientboundSetPlayerTeamPacket.createPlayerPacket(
            new PlayerTeam(null, team.getName()), player.getName(), action),
        viewer);
  }

  @SuppressWarnings({"resource", "OptionalOfNullableMisuse"})
  private Optional<ChunkMap.TrackedEntity> trackedEntity(Player player) {
    ServerLevel world = handle(player).level();
    return Optional.ofNullable(world.getChunkSource().chunkMap.entityMap.get(player.getEntityId()));
  }

  private void send(Packet<?> packet, Player viewer) {
    handle(viewer).connection.send(packet);
  }

  private static ServerPlayer handle(Player player) {
    return ((CraftPlayer) player).getHandle();
  }

  private Set<UUID> viewersOf(Player player) {
    UUID playerId = player.getUniqueId();
    var viewers = new HashSet<UUID>();
    for (var identities : List.of(names, displayNames, skins)) {
      Map<UUID, ?> byViewer = identities.get(playerId);
      if (byViewer != null) viewers.addAll(byViewer.keySet());
    }
    return viewers;
  }

  private void forget(UUID playerId, UUID viewerId) {
    set(names, playerId, viewerId, null);
    set(displayNames, playerId, viewerId, null);
    set(skins, playerId, viewerId, null);
  }

  private void reindex(Player player) {
    UUID playerId = player.getUniqueId();
    if (isDisguised(playerId)) {
      disguised.put(player.getName(), playerId);
    } else {
      disguised.remove(player.getName(), playerId);
    }
  }

  private boolean isDisguised(UUID playerId) {
    return names.containsKey(playerId)
        || displayNames.containsKey(playerId)
        || skins.containsKey(playerId);
  }

  private void prune() {
    names.values().removeIf(Map::isEmpty);
    displayNames.values().removeIf(Map::isEmpty);
    skins.values().removeIf(Map::isEmpty);
    disguised.values().removeIf(playerId -> !isDisguised(playerId));
  }

  private static <K, V> @Nullable V get(Map<UUID, Map<K, V>> identities, UUID id, K key) {
    Map<K, V> inner = identities.get(id);
    return inner == null ? null : inner.get(key);
  }

  private static <K, V> void set(
      Map<UUID, Map<K, V>> identities, UUID id, K key, @Nullable V value) {
    if (value == null) {
      Map<K, V> inner = identities.get(id);
      if (inner != null && inner.remove(key) != null && inner.isEmpty()) {
        identities.remove(id, inner);
      }
    } else {
      identities.computeIfAbsent(id, _ -> new ConcurrentHashMap<>()).put(key, value);
    }
  }
}

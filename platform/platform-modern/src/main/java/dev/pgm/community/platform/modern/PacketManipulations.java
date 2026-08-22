package dev.pgm.community.platform.modern;

import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams.TeamMode;
import dev.pgm.community.util.PlayerIdentities;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import tc.oc.pgm.util.nms.packets.PacketEventsUtil;
import tc.oc.pgm.util.skin.Skin;

@NullMarked
public class PacketManipulations {

  public static final LegacyComponentSerializer LEGACY_AMPERSAND =
      LegacyComponentSerializer.legacySection();

  private final ModernPlayerIdentities identities;
  private final List<PacketListenerCommon> listeners;

  public PacketManipulations() {
    this.identities = (ModernPlayerIdentities) PlayerIdentities.IDENTITIES;

    this.listeners = List.of(PacketEventsUtil.registerSend(
        PacketListenerPriority.LOWEST,
        Map.of(
            PacketType.Play.Server.PLAYER_INFO_UPDATE, this::handlePlayerInfo,
            PacketType.Play.Server.TEAMS, this::handleScoreboardTeams)));
  }

  public void unregister() {
    for (PacketListenerCommon listener : listeners) {
      PacketEventsUtil.unregister(listener);
    }
  }

  private void handlePlayerInfo(PacketSendEvent event) {
    Player viewer = event.getPlayer();
    if (viewer == null) return;
    UUID viewerId = viewer.getUniqueId();

    var wrapper = new WrapperPlayServerPlayerInfoUpdate(event);
    var actions = wrapper.getActions();
    boolean hasAddPlayer = actions.contains(WrapperPlayServerPlayerInfoUpdate.Action.ADD_PLAYER);
    boolean hasUpdateDisplayName =
        actions.contains(WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_DISPLAY_NAME);
    if (!hasAddPlayer && !hasUpdateDisplayName) return;

    boolean modified = false;
    for (var entry : wrapper.getEntries()) {
      UUID playerId = entry.getProfileId();
      if (playerId.equals(viewerId)) continue;

      if (hasAddPlayer) {
        var profile = entry.getGameProfile();

        String name = identities.fakeName(playerId, viewerId);
        if (name != null) {
          profile.setName(name);
          modified = true;
        }

        Skin skin = identities.fakeSkin(playerId, viewerId);
        if (skin != null) {
          var textures = profile.getTextureProperties();
          textures.clear();
          textures.add(new TextureProperty("textures", skin.getData(), skin.getSignature()));
          modified = true;
        }
      }

      if (hasUpdateDisplayName) {
        String displayName = identities.fakeDisplayName(playerId, viewerId);
        if (displayName != null) {
          entry.setDisplayName(LEGACY_AMPERSAND.deserialize(displayName));
          modified = true;
        }
      }
    }

    if (modified) event.markForReEncode(true);
  }

  private void handleScoreboardTeams(PacketSendEvent event) {
    Player viewer = event.getPlayer();
    if (viewer == null || event.isCancelled()) return;
    UUID viewerId = viewer.getUniqueId();

    var wrapper = new WrapperPlayServerTeams(event);
    TeamMode mode = wrapper.getTeamMode();
    String team = wrapper.getTeamName();

    if (mode == TeamMode.REMOVE) {
      identities.teamRemoved(viewerId, team);
      return;
    }

    var entries = wrapper.getPlayers();
    if (entries.isEmpty()) return;

    boolean removing = mode == TeamMode.REMOVE_ENTITIES;
    var rewritten = new ArrayList<String>(entries.size());
    boolean modified = false;

    for (String entry : entries) {
      String sent;
      if (removing) {
        sent = identities.removedName(viewerId, team, entry);
        if (sent == null) {
          modified = true;
          continue;
        }
      } else {
        UUID playerId = identities.disguisedId(entry);
        String name = playerId == null ? null : identities.fakeName(playerId, viewerId);

        sent = name != null ? name : entry;
        identities.renamed(viewerId, team, entry, sent);
      }

      if (!sent.equals(entry)) modified = true;
      rewritten.add(sent);
    }

    if (rewritten.isEmpty()) {
      event.setCancelled(true);
      return;
    }

    if (modified) {
      wrapper.setPlayers(rewritten);
      event.markForReEncode(true);
    }
  }
}

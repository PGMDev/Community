package dev.pgm.community.platform.modern;

import static dev.pgm.community.nick.identity.PlayerIdentity.PLAYER_IDENTITY;

import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams.TeamMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import tc.oc.pgm.util.nms.packets.PacketEventsUtil;
import tc.oc.pgm.util.skin.Skin;

public class PacketManipulations {
  private final PacketListenerCommon listener;

  public PacketManipulations() {
    this.listener = PacketEventsUtil.registerSend(
        PacketListenerPriority.LOWEST,
        Map.of(
            PacketType.Play.Server.PLAYER_INFO_UPDATE,
            this::handlePlayerInfo,
            PacketType.Play.Server.TEAMS,
            this::handleScoreboardTeams));
  }

  public void unregister() {
    PacketEventsUtil.unregister(listener);
  }

  private void handlePlayerInfo(@NonNull PacketSendEvent event) {
    Player viewer = event.getPlayer();
    var wrapper = new WrapperPlayServerPlayerInfoUpdate(event);

    var actions = wrapper.getActions();
    boolean hasAddPlayer = actions.contains(WrapperPlayServerPlayerInfoUpdate.Action.ADD_PLAYER);
    boolean hasUpdateDisplayName =
        actions.contains(WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_DISPLAY_NAME);

    if (!hasAddPlayer && !hasUpdateDisplayName) return;

    boolean modified = false;
    var entries = wrapper.getEntries();
    for (var entry : entries) {
      Player player = Bukkit.getPlayer(entry.getProfileId());
      if (player == null || player.equals(viewer)) continue;

      boolean hasName = PLAYER_IDENTITY.hasName(player, viewer);
      boolean hasSkin = PLAYER_IDENTITY.hasSkin(player, viewer);
      boolean hasDisplayName = PLAYER_IDENTITY.hasDisplayName(player, viewer);

      if (!hasName && !hasSkin && !hasDisplayName) continue;

      if (hasAddPlayer && (hasName || hasSkin)) {
        UserProfile profile = entry.getGameProfile();
        if (hasName) {
          String playerName = PLAYER_IDENTITY.getName(player, viewer);
          if (StringUtils.isBlank(playerName)) continue;

          profile.setName(playerName);
        }

        if (hasSkin) {
          Skin skin = PLAYER_IDENTITY.getSkin(player, viewer);
          profile.getTextureProperties().clear();
          profile
              .getTextureProperties()
              .add(new TextureProperty("textures", skin.getData(), skin.getSignature()));
        }

        entry.setGameProfile(profile);
      }

      if (hasUpdateDisplayName && hasDisplayName) {
        String playerDisplayName = PLAYER_IDENTITY.getDisplayName(player, viewer);
        if (StringUtils.isBlank(playerDisplayName)) continue;

        entry.setDisplayName(
            LegacyComponentSerializer.legacySection().deserialize(playerDisplayName));
      }

      modified = true;
    }

    if (modified) event.markForReEncode(true);
  }

  private void handleScoreboardTeams(@NonNull PacketSendEvent event) {
    Player viewer = event.getPlayer();
    UUID viewerId = viewer.getUniqueId();
    var wrapper = new WrapperPlayServerTeams(event);
    TeamMode mode = wrapper.getTeamMode();
    String teamName = wrapper.getTeamName();

    List<String> players = new ArrayList<>(wrapper.getPlayers());
    if (players.isEmpty()) {
      trackTeamPacket(viewerId, teamName, mode, players);
      return;
    }

    boolean modified = false;
    for (int i = 0; i < players.size(); i++) {
      String entry = players.get(i);
      String visibleName = PLAYER_IDENTITY.getVisibleName(viewerId, entry);
      if (mode == TeamMode.REMOVE_ENTITIES) {
        if (!StringUtils.isBlank(visibleName)
            && PLAYER_IDENTITY.hasTeamEntry(viewerId, teamName, visibleName)) {
          if (!visibleName.equals(entry)) {
            players.set(i, visibleName);
            modified = true;
          }
          continue;
        }

        if (PLAYER_IDENTITY.hasTeamEntry(viewerId, teamName, entry)) continue;

        players.remove(i--);
        modified = true;
        continue;
      }

      if (visibleName == null || visibleName.equals(entry)) continue;

      players.set(i, visibleName);
      modified = true;
    }

    if (mode == TeamMode.REMOVE_ENTITIES && players.isEmpty()) {
      event.setCancelled(true);
      return;
    }

    if (modified) {
      wrapper.setPlayers(players);
      event.markForReEncode(true);
    }

    trackTeamPacket(viewerId, teamName, mode, players);
  }

  private void trackTeamPacket(
      UUID viewerId, String teamName, TeamMode mode, List<String> players) {
    switch (mode) {
      case CREATE -> PLAYER_IDENTITY.createTeam(viewerId, teamName, players);
      case REMOVE -> PLAYER_IDENTITY.removeTeam(viewerId, teamName);
      case ADD_ENTITIES -> PLAYER_IDENTITY.addTeamEntries(viewerId, teamName, players);
      case REMOVE_ENTITIES -> PLAYER_IDENTITY.removeTeamEntries(viewerId, teamName, players);
      case UPDATE -> {
        // Team metadata update; membership is unchanged.
      }
    }
  }
}

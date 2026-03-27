package dev.pgm.community.platform.modern;

import static dev.pgm.community.util.PlayerUtils.PLAYER_UTILS;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedSignedProperty;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import tc.oc.pgm.platform.modern.packets.PacketSender;
import tc.oc.pgm.platform.modern.util.Packets;
import tc.oc.pgm.util.skin.Skin;

public class PacketManipulations implements PacketSender {

  public PacketManipulations(Plugin plugin) {
    Packets.register(
        plugin,
        ListenerPriority.LOWEST,
        Map.of(PacketType.Play.Server.PLAYER_INFO, this::handlePlayerInfo));
  }

  private void handlePlayerInfo(PacketEvent event) {
    Player viewer = event.getPlayer();

    Set<EnumWrappers.PlayerInfoAction> actions =
        event.getPacket().getPlayerInfoActions().read(0);
    boolean hasAddPlayer = actions.contains(EnumWrappers.PlayerInfoAction.ADD_PLAYER);
    boolean hasUpdateDisplayName =
        actions.contains(EnumWrappers.PlayerInfoAction.UPDATE_DISPLAY_NAME);

    if (!hasAddPlayer && !hasUpdateDisplayName) return;

    List<PlayerInfoData> infoList = event.getPacket().getPlayerInfoDataLists().read(0);
    for (int i = 0; i < infoList.size(); i++) {
      PlayerInfoData playerInfoData = infoList.get(i);
      if (playerInfoData == null) continue;

      UUID playerId = playerInfoData.getProfileId();
      Player player = Bukkit.getPlayer(playerId);
      if (player == null || player.equals(viewer) || !player.isOnline()) continue;

      String playerDisplayName = PLAYER_UTILS.getPlayerDisplayName(player, viewer);
      String playerName = PLAYER_UTILS.getPlayerName(player, viewer);

      if (StringUtils.isBlank(playerName) || StringUtils.isBlank(playerDisplayName)) continue;

      WrappedGameProfile wrappedGameProfile = playerInfoData.getProfile().withName(playerName);
      if (hasAddPlayer) {
        Skin playerSkin = PLAYER_UTILS.getPlayerSkin(player, viewer);
        wrappedGameProfile
            .getProperties()
            .put(
                "textures",
                new WrappedSignedProperty(
                    "textures", playerSkin.getData(), playerSkin.getSignature()));
      }

      infoList.set(
          i,
          new PlayerInfoData(
              playerId,
              playerInfoData.getLatency(),
              playerInfoData.isListed(),
              playerInfoData.getGameMode(),
              wrappedGameProfile,
              WrappedChatComponent.fromLegacyText(playerDisplayName)));
    }
  }
}

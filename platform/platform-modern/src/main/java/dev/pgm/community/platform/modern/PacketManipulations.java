package dev.pgm.community.platform.modern;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import dev.pgm.community.util.PlayerUtils;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import tc.oc.pgm.platform.modern.packets.PacketSender;
import tc.oc.pgm.platform.modern.util.Packets;

public class PacketManipulations implements PacketSender {

  public PacketManipulations(Plugin plugin) {
    Packets.register(
        plugin,
        ListenerPriority.LOWEST,
        Map.of(PacketType.Play.Server.PLAYER_INFO, this::handlePlayerInfo));
  }

  private void handlePlayerInfo(PacketEvent event) {
    Player viewer = event.getPlayer();

    if (event
        .getPacket()
        .getPlayerInfoActions()
        .read(0)
        .contains(EnumWrappers.PlayerInfoAction.ADD_PLAYER)) {
      List<PlayerInfoData> infoList = event.getPacket().getPlayerInfoDataLists().read(1);
      event
          .getPacket()
          .getPlayerInfoDataLists()
          .write(
              1,
              infoList.stream()
                  .map((playerInfoData -> {
                    UUID playerId = playerInfoData.getProfileId();
                    Player player = Bukkit.getPlayer(playerId);
                    if (player == null || player.equals(viewer) || !player.isOnline()) {
                      return playerInfoData;
                    }

                    String playerDisplayName =
                        PlayerUtils.PLAYER_UTILS.getPlayerDisplayName(player, viewer);
                    String playerName = PlayerUtils.PLAYER_UTILS.getPlayerName(player, viewer);

                    if (StringUtils.isBlank(playerName) || StringUtils.isBlank(playerDisplayName)) {
                      return playerInfoData;
                    }

                    WrappedGameProfile playerProfile =
                        playerInfoData.getProfile().withName(playerName);
                    playerInfoData.getProfile().getProperties().forEach((key, property) -> {
                      playerProfile.getProperties().put(key, property);
                    });

                    return new PlayerInfoData(
                        playerId,
                        playerInfoData.getLatency(),
                        playerInfoData.isListed(),
                        playerInfoData.getGameMode(),
                        playerProfile,
                        WrappedChatComponent.fromLegacyText(playerDisplayName));
                  }))
                  .toList());
    }
  }
}

package dev.pgm.community.platform.modern;

import static dev.pgm.community.util.PlayerUtils.PLAYER_UTILS;

import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import tc.oc.pgm.platform.modern.util.Packets;
import tc.oc.pgm.util.skin.Skin;

public class PacketManipulations {

  public PacketManipulations() {
    Packets.registerSend(
        PacketListenerPriority.LOWEST,
        Map.of(PacketType.Play.Server.PLAYER_INFO_UPDATE, this::handlePlayerInfo));
  }

  private void handlePlayerInfo(@NonNull PacketSendEvent event) {
    Player viewer = event.getPlayer();
    WrapperPlayServerPlayerInfoUpdate wrapper = new WrapperPlayServerPlayerInfoUpdate(event);

    boolean modified = false;
    List<WrapperPlayServerPlayerInfoUpdate.PlayerInfo> entries = wrapper.getEntries();
    for (WrapperPlayServerPlayerInfoUpdate.PlayerInfo entry : entries) {
      Player player = Bukkit.getPlayer(entry.getProfileId());
      if (player == null || player.equals(viewer) || !player.isOnline()) continue;

      String playerDisplayName = PLAYER_UTILS.getPlayerDisplayName(player, viewer);
      String playerName = PLAYER_UTILS.getPlayerName(player, viewer);

      if (StringUtils.isBlank(playerName) || StringUtils.isBlank(playerDisplayName)) continue;

      UserProfile profile = entry.getGameProfile();
      profile.setName(playerName);

      Skin skin = PLAYER_UTILS.getPlayerSkin(player, viewer);
      profile.getTextureProperties().clear();
      profile
          .getTextureProperties()
          .add(new TextureProperty("textures", skin.getData(), skin.getSignature()));

      entry.setGameProfile(profile);
      entry.setDisplayName(
          LegacyComponentSerializer.legacySection().deserialize(playerDisplayName));

      modified = true;
    }

    if (modified) event.markForReEncode(true);
  }
}

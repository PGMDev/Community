package dev.pgm.community.platform.sportpaper.features.serverlinks;

import static tc.oc.pgm.util.Assert.assertNotNull;

import com.viaversion.nbt.tag.Tag;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.Protocol;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.packet.State;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.libs.gson.JsonParser;
import com.viaversion.viaversion.libs.mcstructs.text.utils.JsonNbtConverter;
import com.viaversion.viaversion.protocols.v1_20to1_20_2.storage.ConfigurationState;
import dev.pgm.community.Community;
import dev.pgm.community.serverlinks.types.ServerLink;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class ViaServerLinks {
  private static final Protocol<?, ?, ?, ?> serverLinkProtocol = findServerLinkProtocol();

  public static void sendToPlayer(Player player, List<ServerLink> serverLinks) {
    if (!Via.getAPI().isInjected(player.getUniqueId())) return;
    UserConnection userConnection = Via.getAPI().getConnection(player.getUniqueId());
    if (userConnection != null
        && userConnection
            .getProtocolInfo()
            .protocolVersion()
            .newerThanOrEqualTo(ProtocolVersion.v1_21)) {
      PacketWrapper serverLinksPacket = createPacket(userConnection, serverLinks);
      var maybeConfigState = userConnection.get(ConfigurationState.class);
      assert maybeConfigState != null; // will be present for 1.21+ connections
      if (maybeConfigState.bridgePhase() != ConfigurationState.BridgePhase.NONE) {
        // The client is in config phase, queue the packet up so the client doesn't crash
        // This does get sent through the 1.20 -> 1.20.2 protocol internally, and thus is
        // technically wrong, but it seems to go through and upgrade to the client's version just
        // fine.
        Community.get()
            .getLogger()
            .info("Queuing SERVER_LINKS packet for player " + player
                + " because they are still in config phase");
        maybeConfigState.addClientboundPacketToQueue(serverLinksPacket);
      } else {
        serverLinksPacket.scheduleSend(serverLinkProtocol.getClass());
      }
    }
  }

  private static PacketWrapper createPacket(UserConnection conn, List<ServerLink> links) {
    var packetTypes = serverLinkProtocol.getPacketTypesProvider().mappedClientboundPacketTypes();
    var packetType = packetTypes.get(State.PLAY).typeByName("SERVER_LINKS");
    PacketWrapper packet = PacketWrapper.create(packetType, conn);
    packet.write(Types.VAR_INT, links.size());
    for (ServerLink link : links) {
      packet.write(Types.BOOLEAN, link.builtinType() != null);
      if (link.builtinType() != null) {
        packet.write(Types.VAR_INT, link.builtinType().ordinal());
      } else {
        assert link.customText() != null;
        packet.write(Types.TAG, toViaTag(link.customText()));
      }
      packet.write(Types.STRING, link.uri().toString());
    }

    return packet;
  }

  private static Tag toViaTag(Component component) {
    return assertNotNull(
        JsonNbtConverter.toNbt(
            JsonParser.parseString(GsonComponentSerializer.gson().serialize(component))),
        "Component -> NBT conversion failed");
  }

  private static Protocol<?, ?, ?, ?> findServerLinkProtocol() {
    return assertNotNull(
        Via.getManager()
            .getProtocolManager()
            .getProtocol(/* to */ ProtocolVersion.v1_21, /* from */ ProtocolVersion.v1_20_5),
        "ViaVersion v1.20.5 -> v1.21 protocol was not found");
  }
}

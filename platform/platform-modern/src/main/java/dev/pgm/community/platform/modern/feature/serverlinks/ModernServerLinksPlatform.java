package dev.pgm.community.platform.modern.feature.serverlinks;

import static dev.pgm.community.util.Supports.Variant.PAPER;

import dev.pgm.community.serverlinks.ServerLinksFeature;
import dev.pgm.community.serverlinks.types.ServerLink;
import dev.pgm.community.serverlinks.types.ServerLinkBuiltinType;
import dev.pgm.community.util.Supports;
import java.util.List;
import org.bukkit.ServerLinks;
import org.bukkit.craftbukkit.CraftServerLinks;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@Supports(PAPER)
@NullMarked
public class ModernServerLinksPlatform implements ServerLinksFeature.ServerLinksPlatform {
  @Override
  public void sendToPlayer(Player player, List<ServerLink> serverLinks) {
    player.sendLinks(toPlatformServerLinks(serverLinks));
  }

  private ServerLinks toPlatformServerLinks(List<ServerLink> links) {
    ServerLinks bukkitLinks = new CraftServerLinks(new net.minecraft.server.ServerLinks(List.of()));
    for (ServerLink link : links) {
      if (link.builtinType() != null) {
        bukkitLinks.addLink(toBukkitType(link.builtinType()), link.uri());
      } else {
        assert link.customText() != null;
        bukkitLinks.addLink(link.customText(), link.uri());
      }
    }

    return bukkitLinks;
  }

  private ServerLinks.Type toBukkitType(ServerLinkBuiltinType type) {
    return ServerLinks.Type.values()[type.ordinal()];
  }
}

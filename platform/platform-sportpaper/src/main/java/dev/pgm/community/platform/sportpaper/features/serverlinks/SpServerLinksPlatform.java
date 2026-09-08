package dev.pgm.community.platform.sportpaper.features.serverlinks;

import static dev.pgm.community.util.Supports.Variant.SPORTPAPER;

import dev.pgm.community.serverlinks.ServerLinksFeature;
import dev.pgm.community.serverlinks.types.ServerLink;
import dev.pgm.community.util.Supports;
import java.util.List;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@Supports(SPORTPAPER)
@NullMarked
public class SpServerLinksPlatform implements ServerLinksFeature.ServerLinksPlatform {
  private static final boolean HAS_VIA = hasVia();

  private static boolean hasVia() {
    try {
      Class.forName("com.viaversion.viaversion.api.Via");
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  @Override
  public boolean isSupported() {
    return HAS_VIA;
  }

  @Override
  public void sendToPlayer(Player player, List<ServerLink> serverLinks) {
    if (HAS_VIA) {
      ViaServerLinks.sendToPlayer(player, serverLinks);
    }
  }
}

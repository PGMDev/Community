package dev.pgm.community.serverlinks;

import dev.pgm.community.feature.FeatureBase;
import dev.pgm.community.serverlinks.types.ServerLink;
import dev.pgm.community.util.Platform;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class ServerLinksFeature extends FeatureBase {
  private static final ServerLinksPlatform PLATFORM = Platform.get(ServerLinksPlatform.class);

  public interface ServerLinksPlatform {
    default boolean isSupported() {
      return true;
    }

    void sendToPlayer(Player player, List<ServerLink> serverLinks);
  }

  public ServerLinksFeature(Configuration config, Logger logger) {
    super(new ServerLinksConfig(config), logger, "Server Links");

    if (getConfig().isEnabled()) {
      if (!PLATFORM.isSupported()) {
        logger.warning("Server links are enabled but not supported by the platform");
        return;
      }
      enable();
    }
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    PLATFORM.sendToPlayer(event.getPlayer(), getServerLinksConfig().getLinks());
  }

  public ServerLinksConfig getServerLinksConfig() {
    return (ServerLinksConfig) getConfig();
  }
}

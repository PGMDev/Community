package dev.pgm.community.info;

import dev.pgm.community.feature.FeatureBase;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.bukkit.configuration.Configuration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

/** InfoCommandsFeature - Allows for commands via the config to be defined and used * */
public class InfoCommandsFeature extends FeatureBase {

  public InfoCommandsFeature(Configuration config, Logger logger) {
    super(new InfoCommandConfig(config), logger, "Info Commands");
    enable();
  }

  public InfoCommandConfig getInfoConfig() {
    return (InfoCommandConfig) getConfig();
  }

  @EventHandler
  public void onPlayerCommandProcess(PlayerCommandPreprocessEvent event) {
    // We dynamically check for defined commands, and send the related feedback
    getInfoConfig().getInfoCommands().stream()
        .filter(c -> event.getMessage().toLowerCase().startsWith("/" + c.getName().toLowerCase()))
        .findAny()
        .ifPresent(
            command -> {
              command.sendCommand(event.getPlayer());
              event.setCancelled(true);
            });
  }

  @Override
  public CompletableFuture<Integer> count() {
    return CompletableFuture.completedFuture(getInfoConfig().getInfoCommands().size());
  }
}

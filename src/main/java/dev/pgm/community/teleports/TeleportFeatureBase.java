package dev.pgm.community.teleports;

import com.google.common.collect.Sets;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.feature.FeatureBase;
import dev.pgm.community.utils.CommandAudience;
import dev.pgm.community.utils.Sounds;
import java.util.Set;
import java.util.logging.Logger;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import tc.oc.pgm.util.chat.Audience;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.types.PlayerComponent;

public class TeleportFeatureBase extends FeatureBase implements TeleportFeature {

  public TeleportFeatureBase(Configuration config, Logger logger) {
    super(new TeleportConfig(config), logger);
  }

  public TeleportConfig getTeleportConfig() {
    return (TeleportConfig) getConfig();
  }

  @Override
  public Set<CommunityCommand> getCommands() {
    return getTeleportConfig().isEnabled()
        ? Sets.newHashSet(new TeleportCommand())
        : Sets.newHashSet();
  }

  @Override
  public void teleport(
      CommandAudience sender, Player teleporter, Location target, Component message) {
    boolean involved =
        sender.isPlayer()
            && (sender.getPlayer().equals(teleporter) || sender.getPlayer().equals(teleporter));

    teleporter.teleport(target);
    if (message != null) {
      sendTeleportMessage(teleporter, message);
    }

    if (!involved) {
      sender.sendMessage(
          TextComponent.builder()
              .append("Teleported ")
              .append(PlayerComponent.of(teleporter, NameStyle.FANCY))
              .append(" to ")
              .append(formatLocation(target))
              .color(TextColor.GRAY)
              .build());
    }
  }

  @Override
  public void teleport(
      CommandAudience sender,
      Player teleporter,
      Player target,
      Component teleporterMsg,
      Component targetMsg,
      boolean senderFeedback) {
    boolean involved =
        sender.isPlayer()
            && (sender.getPlayer().equals(teleporter) || sender.getPlayer().equals(target));

    teleporter.teleport(target);

    if (teleporterMsg != null) {
      sendTeleportMessage(teleporter, teleporterMsg);
    }

    if (targetMsg != null) {
      sendTeleportMessage(target, targetMsg);
    }

    if (senderFeedback && !involved) {
      sender.sendMessage(
          TextComponent.builder()
              .append("Teleported ")
              .append(PlayerComponent.of(teleporter, NameStyle.FANCY))
              .append(" to ")
              .append(PlayerComponent.of(target, NameStyle.FANCY))
              .color(TextColor.GRAY)
              .build());
    }
  }

  private void sendTeleportMessage(Player teleporter, Component message) {
    Audience viewer = Audience.get(teleporter);
    viewer.sendMessage(message);
    if (getTeleportConfig().isSoundPlayed()) {
      viewer.playSound(Sounds.TELEPORT);
    }
  }
}

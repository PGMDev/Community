package dev.pgm.community.teleports;

import static net.kyori.adventure.text.Component.text;
import static tc.oc.pgm.util.player.PlayerComponent.player;

import com.google.common.collect.Sets;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.feature.FeatureBase;
import dev.pgm.community.utils.CommandAudience;
import dev.pgm.community.utils.Sounds;
import java.util.Set;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.named.NameStyle;

public class TeleportFeatureBase extends FeatureBase implements TeleportFeature {

  public TeleportFeatureBase(Configuration config, Logger logger) {
    super(new TeleportConfig(config), logger, "Teleports");
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
          text("Teleported ")
              .append(player(teleporter, NameStyle.FANCY))
              .append(text(" to "))
              .append(formatLocation(target))
              .color(NamedTextColor.GRAY));
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
          text("Teleported ")
              .append(player(teleporter, NameStyle.FANCY))
              .append(text(" to "))
              .append(player(target, NameStyle.FANCY))
              .color(NamedTextColor.GRAY));
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

package dev.pgm.community.moderation.commands;

import static net.kyori.adventure.text.Component.text;
import static tc.oc.pgm.util.text.TextException.exception;

import dev.pgm.community.Community;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.moderation.feature.loggers.BlockGlitchLogger;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Argument;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Command;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Permission;

public class BlockGlitchCommand {
  private static final int OBS_ROTATE_DISTANCE = 15;
  private static final int PLAY_ROTATE_DISTANCE = 40;

  private final BlockGlitchLogger blockGlitch;

  public BlockGlitchCommand() {
    this.blockGlitch = Community.get().getFeatures().getModeration().getBlockGlitchLogger();
  }

  @Command("blockglitch list")
  @Permission(CommunityPermissions.BLOCK_GLITCH_BROADCASTS)
  public void listIncidents(MatchPlayer player) {
    var incidents = blockGlitch.getIncidents();
    if (incidents.isEmpty()) throw exception("No recorded blockglitch incidents");

    for (var incident : incidents) {
      player.sendMessage(incident.getDescription());
    }
  }

  @Command("blockglitch replay <id>")
  @Permission(CommunityPermissions.BLOCK_GLITCH_BROADCASTS)
  public void replayIncident(MatchPlayer player, @Argument("id") int id) {
    var incident = blockGlitch.getIncident(id);
    if (incident == null) throw exception("Sorry, the block glitch happened too long ago to view");

    Location curr = player.getLocation(), start = incident.getStart();
    int distance = (int) Math.min(curr.distance(start), curr.distance(incident.getEnd()));

    if (distance < (player.isObserving() ? OBS_ROTATE_DISTANCE : PLAY_ROTATE_DISTANCE)) {
      player.getBukkit().teleport(setFacing(curr, start));
    } else if (player.isObserving()) {
      player.getBukkit().teleport(moveAway(start));
    } else {
      throw exception("Join observers or get closer to watch this replay");
    }

    player.sendMessage(text("Replaying ", NamedTextColor.GRAY)
        .append(incident.getPlayerName())
        .append(text(" blockglitching "))
        .append(incident.getWhen().color(NamedTextColor.YELLOW)));
    incident.play(player.getBukkit());
  }

  private static Location setFacing(Location base, Location target) {
    double dx = target.getX() - base.getX();
    double dy = target.getY() - base.getY();
    double dz = target.getZ() - base.getZ();
    double horizDist = Math.sqrt(dx * dx + dz * dz);

    base.setPitch((float) Math.toDegrees(Math.atan2(-dy, horizDist)));
    base.setYaw((float) Math.toDegrees(Math.atan2(-dx, dz)));
    return base;
  }

  private static Location moveAway(Location base) {
    Vector direction = base.getDirection();
    base.setY(base.getY() + 1.63); // Add eye height
    for (int i = 0; i < 3; i++) {
      base.subtract(direction);
      if (!base.getBlock().isEmpty()) {
        base.add(direction);
        break;
      }
    }
    base.setY(base.getY() - 1.63);
    return base;
  }
}

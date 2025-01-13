package dev.pgm.community.moderation.commands;

import static net.kyori.adventure.text.Component.text;
import static tc.oc.pgm.util.text.TextException.exception;

import dev.pgm.community.Community;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.moderation.feature.loggers.BlockGlitchLogger;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Argument;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Command;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Permission;

public class BlockGlitchCommand {
  private static final int TP_DISTANCE = 15;
  private static final int MAX_NO_OBS_DISTANCE = 40;

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
    Location curr = player.getLocation();
    int distance =
        (int) Math.min(curr.distance(incident.getStart()), curr.distance(incident.getEnd()));

    if (player.isObserving()) {
      if (distance > TP_DISTANCE) player.getBukkit().teleport(incident.getStart());
    } else if (distance > MAX_NO_OBS_DISTANCE) {
      throw exception("Join observers or get closer to watch this replay");
    }

    player.sendMessage(text("Replaying ", NamedTextColor.GRAY)
        .append(incident.getPlayerName())
        .append(text(" blockglitching "))
        .append(incident.getWhen().color(NamedTextColor.YELLOW)));
    incident.play(player.getBukkit());
  }
}

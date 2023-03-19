package dev.pgm.community.commands;

import static net.kyori.adventure.text.Component.text;

import dev.pgm.community.CommunityCommand;
import dev.pgm.community.utils.CommandAudience;
import java.time.Duration;
import java.time.Instant;
import net.kyori.adventure.text.format.NamedTextColor;
import tc.oc.pgm.lib.cloud.commandframework.annotations.CommandDescription;
import tc.oc.pgm.lib.cloud.commandframework.annotations.CommandMethod;
import tc.oc.pgm.util.text.TemporalComponent;

public class ServerInfoCommand extends CommunityCommand {

  private final Instant startTime;

  public ServerInfoCommand() {
    this.startTime = Instant.now();
  }

  @CommandMethod("uptime")
  @CommandDescription("View how long the server has been online")
  public void uptime(CommandAudience sender) {
    Duration uptime = Duration.between(startTime, Instant.now());
    sender.sendMessage(
        text()
            .append(text("Server has been online for "))
            .append(TemporalComponent.duration(uptime, NamedTextColor.GREEN))
            .color(NamedTextColor.GRAY)
            .build());
  }
}

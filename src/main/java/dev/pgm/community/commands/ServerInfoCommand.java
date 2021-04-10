package dev.pgm.community.commands;

import static net.kyori.adventure.text.Component.text;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Description;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.utils.CommandAudience;
import java.time.Duration;
import java.time.Instant;
import net.kyori.adventure.text.format.NamedTextColor;
import tc.oc.pgm.util.text.TemporalComponent;

public class ServerInfoCommand extends CommunityCommand {

  @Dependency("startTime")
  private Instant startTime;

  @CommandAlias("uptime")
  @Description("View how long the server has been online")
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

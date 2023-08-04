package dev.pgm.community.polls.commands;

import static net.kyori.adventure.text.Component.text;

import dev.pgm.community.Community;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.mutations.MutationType;
import dev.pgm.community.polls.feature.PollFeature;
import dev.pgm.community.utils.CommandAudience;
import java.time.Duration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.lib.cloud.commandframework.annotations.Argument;
import tc.oc.pgm.lib.cloud.commandframework.annotations.CommandMethod;
import tc.oc.pgm.lib.cloud.commandframework.annotations.CommandPermission;
import tc.oc.pgm.lib.cloud.commandframework.annotations.Flag;
import tc.oc.pgm.lib.cloud.commandframework.annotations.specifier.Greedy;
import tc.oc.pgm.util.text.TextException;

@CommandMethod("poll")
@CommandPermission(CommunityPermissions.POLL)
public class PollManagementCommands extends CommunityCommand {

  private final PollFeature polls;

  public PollManagementCommands() {
    this.polls = Community.get().getFeatures().getPolls();
  }

  @CommandMethod("")
  public void info(CommandAudience audience) {
    if (polls.isRunning()) {
      polls.sendPollDetails(polls.getPoll(), audience);
    } else {
      polls.sendBuilderDetails(polls.getBuilder(), audience);
    }
  }

  @CommandMethod("start [duration]")
  public void start(
      CommandAudience audience,
      @Argument("duration") Duration duration,
      @Flag(value = "delay", aliases = "d") Duration delay) {
    if (polls.isDelayedStartScheduled()) {
      audience.sendWarning(
          text("A poll is already scheduled to start soon!")
              .hoverEvent(
                  HoverEvent.showText(
                      text()
                          .append(text("Use "))
                          .append(text("/poll end", NamedTextColor.AQUA))
                          .append(text(" to cancel delayed start."))
                          .color(NamedTextColor.GRAY)
                          .build())));
      return;
    }

    if (!polls.canStart()) {
      audience.sendWarning(
          text()
              .append(
                  text("Your poll is not ready to start! Check ")
                      .append(text("/poll", NamedTextColor.AQUA))
                      .append(text(" for more info.")))
              .hoverEvent(HoverEvent.showText(text("Click to view poll info")))
              .clickEvent(ClickEvent.runCommand("/poll"))
              .build());
      return;
    }

    if (duration != null) {
      polls.getBuilder().duration(audience, duration);
    }

    polls.getBuilder().creator(audience.getId().orElse(null));
    if (delay == null) {
      polls.start(audience);
    } else {
      polls.delayedStart(audience, delay);
    }
  }

  @CommandMethod("end")
  public void end(CommandAudience audience) {
    polls.end(audience);
  }

  @CommandMethod("question [question]")
  public void question(CommandAudience audience, @Argument("question") @Greedy String question) {
    checkPoll();
    polls.getBuilder().question(audience, question);
  }

  @CommandMethod("duration [duration]")
  public void timelimit(CommandAudience audience, @Argument("duration") Duration duration) {
    checkPoll();
    polls.getBuilder().duration(audience, duration);
  }

  @CommandMethod("map [map]")
  public void map(CommandAudience audience, @Argument("map") MapInfo map) {
    checkPoll();
    polls.getBuilder().map(audience, map);
  }

  @CommandMethod("kick [target]")
  public void kickPlayer(CommandAudience audience, @Argument("target") Player target) {
    checkPoll();
    polls.getBuilder().kickPlayer(audience, target);
  }

  @CommandMethod("command [command]")
  public void command(CommandAudience audience, @Argument("command") @Greedy String command) {
    checkPoll();
    polls.getBuilder().command(audience, command);
  }

  @CommandMethod("mutation [mutation]")
  @CommandPermission(CommunityPermissions.MUTATION)
  public void mutation(CommandAudience audience, @Argument("mutation") MutationType mutation) {
    checkPoll();
    polls.getBuilder().mutation(audience, mutation);
  }

  @CommandMethod("reset")
  public void reset(CommandAudience audience) {
    checkPoll();
    polls.resetBuilder();
    audience.sendWarning(text("Poll values have been reset!"));
  }

  private void checkPoll() {
    if (polls.isRunning()) {
      throw TextException.exception("Poll can not be adjusted at this time!");
    }
  }
}

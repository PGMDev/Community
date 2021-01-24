package dev.pgm.community.poll.commands;

import static net.kyori.adventure.text.Component.text;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Description;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.poll.feature.PollFeature;
import dev.pgm.community.utils.CommandAudience;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public class VoteCommand extends CommunityCommand {

  @Dependency private PollFeature polls;

  @CommandAlias("yes|ya")
  @Description("Vote yes in the current poll")
  public void yes(CommandAudience audience, Player player) {
    if (vote(audience, player, true)) {
      audience.sendMessage(
          text()
              .append(
                  text("Voted ")
                      .append(text("yes", NamedTextColor.GREEN))
                      .append(text(" for current poll"))
                      .color(NamedTextColor.GRAY))
              .build());
    }
  }

  @CommandAlias("no|nay")
  @Description("Vote note in the current poll")
  public void no(CommandAudience audience, Player player) {
    if (vote(audience, player, false)) {
      audience.sendMessage(
          text()
              .append(
                  text("Voted ")
                      .append(text("no", NamedTextColor.RED))
                      .append(text(" for current poll"))
                      .color(NamedTextColor.GRAY))
              .build());
    }
  }

  public boolean vote(CommandAudience audience, Player player, boolean yes) {
    if (polls.getPoll() == null || !polls.getPoll().isActive()) {
      audience.sendWarning(text("There is no active poll to vote on"));
      return false;
    }
    polls.getPoll().vote(player, yes);
    return true;
  }
}

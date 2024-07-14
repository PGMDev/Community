package dev.pgm.community.polls.commands;

import dev.pgm.community.Community;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.polls.feature.PollFeature;
import dev.pgm.community.utils.CommandAudience;
import org.bukkit.entity.Player;
import tc.oc.pgm.lib.org.incendo.cloud.annotation.specifier.Greedy;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Argument;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Command;

public class PollVoteCommands extends CommunityCommand {

  public static final String COMMAND = "pollvote";

  private final PollFeature polls;

  public PollVoteCommands() {
    this.polls = Community.get().getFeatures().getPolls();
  }

  @Command(COMMAND + " <option>")
  public void pollVote(
      CommandAudience viewer, Player sender, @Argument("option") @Greedy String option) {
    polls.vote(viewer, sender, option);
  }
}

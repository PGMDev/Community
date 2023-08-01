package dev.pgm.community.polls.commands;

import dev.pgm.community.Community;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.polls.feature.PollFeature;
import dev.pgm.community.utils.CommandAudience;
import org.bukkit.entity.Player;
import tc.oc.pgm.lib.cloud.commandframework.annotations.CommandMethod;

public class PollVoteCommands extends CommunityCommand {

  private final PollFeature polls;

  public PollVoteCommands() {
    this.polls = Community.get().getFeatures().getPolls();
  }

  @CommandMethod("yes")
  public void pollYes(CommandAudience viewer, Player sender) {
    polls.vote(viewer, sender, true);
  }

  @CommandMethod("no")
  public void pollNo(CommandAudience viewer, Player sender) {
    polls.vote(viewer, sender, false);
  }
}

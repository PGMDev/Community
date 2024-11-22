package dev.pgm.community.requests.commands.supervotes;

import dev.pgm.community.Community;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.requests.feature.RequestFeature;
import dev.pgm.community.utils.CommandAudience;
import org.bukkit.entity.Player;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Command;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.CommandDescription;

public class SuperVoteCommand extends CommunityCommand {

  private final RequestFeature requests;

  public SuperVoteCommand() {
    this.requests = Community.get().getFeatures().getRequests();
  }

  @Command("supervote")
  @CommandDescription("Activate a supervote for the active map vote")
  public void superVote(CommandAudience audience, Player sender) {
    requests.superVote(sender);
  }
}

package dev.pgm.community.history;

import static tc.oc.pgm.util.text.TextException.exception;

import dev.pgm.community.Community;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.utils.CommandAudience;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Argument;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Command;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.CommandDescription;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Default;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Flag;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Permission;

@Command("matchhistory|mh")
public class MatchHistoryCommand {

  private final MatchHistoryFeature manager;

  public MatchHistoryCommand() {
    this.manager = Community.get().getFeatures().getHistory();
  }

  @Command("[page]")
  @CommandDescription("Display match history")
  @Permission(CommunityPermissions.MATCH_HISTORY)
  public void sendHistory(
      CommandAudience sender,
      @Argument("page") @Default("1") int page,
      @Flag(value = "verbose", aliases = "v") boolean verbose) {
    checkEnabled();
    manager.sendHistory(sender, page, verbose);
  }

  private void checkEnabled() {
    if (!manager.isEnabled()) {
      throw exception("Match History is not enabled");
    }
  }
}

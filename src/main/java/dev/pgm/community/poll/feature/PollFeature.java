package dev.pgm.community.poll.feature;

import static dev.pgm.community.utils.PGMUtils.isPGMEnabled;

import com.google.common.collect.Sets;
import dev.pgm.community.Community;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.feature.FeatureBase;
import dev.pgm.community.poll.Poll;
import dev.pgm.community.poll.PollConfig;
import dev.pgm.community.poll.PollType;
import dev.pgm.community.poll.commands.PollCommand;
import dev.pgm.community.poll.commands.VoteCommand;
import dev.pgm.community.poll.countdown.PollCountdown;
import dev.pgm.community.poll.types.ExecutablePoll;
import dev.pgm.community.utils.PGMUtils;
import java.util.Set;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.match.Match;

public class PollFeature extends FeatureBase {

  private Poll poll;

  private @Nullable PollCountdown countdown;

  public PollFeature(Configuration config, Logger logger) {
    super(new PollConfig(config), logger);

    if (getConfig().isEnabled() && isPGMEnabled()) {
      enable();

      Community.get()
          .getServer()
          .getScheduler()
          .scheduleSyncRepeatingTask(Community.get(), this::onTask, 0L, 20L);
    }
  }

  public PollConfig getPollConfig() {
    return (PollConfig) getConfig();
  }

  public Poll getPoll() {
    return poll;
  }

  public void createPoll(PollType type, String input, Player executor) {
    switch (type) {
      case COMMAND:
        this.poll =
            new ExecutablePoll(input, getPollConfig().getDefaultLength(), executor.getUniqueId());
        break;
    }
  }

  public boolean start() {
    if (poll != null && !poll.isActive()) {
      poll.start();
      if (PGMUtils.isPGMEnabled()
          && getPollConfig().isIntegrationEnabled()
          && PGMUtils.getMatch() != null) {
        Match match = PGMUtils.getMatch();
        this.countdown = new PollCountdown(poll, match);
        match.getCountdown().start(countdown, poll.getLength());
      }
      return true;
    }
    return false;
  }

  public boolean cancel() {
    if (poll != null) {
      removeCountdown();
      this.poll = null;
      return true;
    }
    return false;
  }

  public boolean end() {
    if (poll != null && poll.isActive()) {
      poll.complete();
      removeCountdown();
      this.poll = null;
      return true;
    }
    return false;
  }

  private void removeCountdown() {
    if (countdown != null) {
      Match match = PGMUtils.getMatch();
      match.getCountdown().cancel(countdown);
      countdown = null;
    }
  }

  private void onTask() {
    if (poll != null && poll.isActive()) {
      if (poll.getTimeLeft().getSeconds() <= 0) {
        end();
      }
    }
  }

  @Override
  public Set<CommunityCommand> getCommands() {
    return getConfig().isEnabled() && isPGMEnabled()
        ? Sets.newHashSet(new PollCommand(), new VoteCommand())
        : Sets.newHashSet();
  }
}

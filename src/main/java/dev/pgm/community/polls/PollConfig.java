package dev.pgm.community.polls;

import static tc.oc.pgm.util.text.TextParser.parseDuration;

import dev.pgm.community.feature.config.FeatureConfigImpl;
import java.time.Duration;
import org.bukkit.configuration.Configuration;

public class PollConfig extends FeatureConfigImpl {

  private static final String KEY = "polls";

  private Duration duration;
  private PollThreshold threshold;

  public PollConfig(Configuration config) {
    super(KEY, config);
  }

  public Duration getDuration() {
    return duration;
  }

  public PollThreshold getThreshold() {
    return threshold;
  }

  @Override
  public void reload(Configuration config) {
    super.reload(config);
    this.duration = parseDuration(config.getString(KEY + ".duration"));
    this.threshold = PollThreshold.valueOf(config.getString(KEY + ".threshold").toUpperCase());
  }
}

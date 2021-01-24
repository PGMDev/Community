package dev.pgm.community.poll;

import dev.pgm.community.feature.config.FeatureConfigImpl;
import java.time.Duration;
import org.bukkit.configuration.Configuration;

public class PollConfig extends FeatureConfigImpl {

  private static final String KEY = "polls";

  private int defaultSeconds;
  private boolean confirm;
  private boolean pgmIntegration;

  public PollConfig(Configuration config) {
    super(KEY, config);
  }

  public Duration getDefaultLength() {
    return Duration.ofSeconds(defaultSeconds);
  }

  public boolean isConfirm() {
    return confirm;
  }

  public boolean isIntegrationEnabled() {
    return pgmIntegration;
  }

  @Override
  public void reload(Configuration config) {
    super.reload(config);
    this.defaultSeconds = config.getInt(KEY + ".default-time");
    this.confirm = config.getBoolean(KEY + ".confirm");
    this.pgmIntegration = config.getBoolean(KEY + ".pgm-integration");
  }
}

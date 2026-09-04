package dev.pgm.community.friends;

import static tc.oc.pgm.util.text.TextParser.parseDuration;

import dev.pgm.community.Community;
import dev.pgm.community.feature.config.FeatureConfigImpl;
import java.time.Duration;
import org.bukkit.configuration.Configuration;
import tc.oc.pgm.util.text.TextException;

public class FriendshipConfig extends FeatureConfigImpl {

  public static final String KEY = "friends";

  private boolean pgmIntegration;
  private Duration requestCooldown;

  public FriendshipConfig(Configuration config) {
    super(KEY, config);
  }

  public boolean isIntegrationEnabled() {
    return pgmIntegration;
  }

  public Duration getRequestCooldown() {
    return requestCooldown;
  }

  @Override
  public void reload(Configuration config) {
    super.reload(config);
    this.pgmIntegration = config.getBoolean(getKey() + ".pgm-integration");
    String cooldownValue = config.getString(getKey() + ".request-cooldown", "1h");
    try {
      this.requestCooldown = parseDuration(cooldownValue);
    } catch (TextException e) {
      this.requestCooldown = Duration.ofHours(1);
      Community.get()
          .getLogger()
          .warning("Invalid friends.request-cooldown value '"
              + cooldownValue
              + "'; using default of 1h");
    }
  }
}

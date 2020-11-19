package dev.pgm.community.chat;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import dev.pgm.community.feature.config.FeatureConfigImpl;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.bukkit.configuration.Configuration;

/** Configuration related to chat management features */
public class ChatManagementConfig extends FeatureConfigImpl {

  public static final String KEY = "chat";

  private int slowmodeSpeed;
  private boolean loginAlerts;

  private Cache<UUID, Instant> lastSentMessage;

  public ChatManagementConfig(Configuration config) {
    super(KEY, config);
  }

  public int getSlowmodeSpeed() {
    return slowmodeSpeed;
  }

  public Cache<UUID, Instant> getLastMessageCache() {
    return lastSentMessage;
  }

  public boolean isLoginAlertsEnabled() {
    return loginAlerts;
  }

  @Override
  public void reload(Configuration config) {
    super.reload(config);
    this.slowmodeSpeed = config.getInt(KEY + ".slowmode-speed");
    this.loginAlerts = config.getBoolean(KEY + ".login-alert");
    this.lastSentMessage =
        CacheBuilder.newBuilder().expireAfterWrite(slowmodeSpeed, TimeUnit.SECONDS).build();
  }
}

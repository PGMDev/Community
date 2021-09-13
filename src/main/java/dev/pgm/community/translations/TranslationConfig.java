package dev.pgm.community.translations;

import dev.pgm.community.feature.config.FeatureConfigImpl;
import java.util.List;
import org.bukkit.configuration.Configuration;

public class TranslationConfig extends FeatureConfigImpl {

  private static final String KEY = "translations";
  private static final String CONNECT_TIMEOUT = KEY + ".connect-timeout";
  private static final String READ_TIMEOUT = KEY + ".read-timeout";
  private static final String LANGUAGES = KEY + ".languages";

  private int connectTimeoutSeconds;
  private int readTimeoutSeconds;
  private List<String> languages;

  public TranslationConfig(Configuration config) {
    super(KEY, config);
  }

  public int getConnectTimeout() {
    return connectTimeoutSeconds;
  }

  public int getReadTimeout() {
    return readTimeoutSeconds;
  }

  public List<String> getLanguages() {
    return languages;
  }

  @Override
  public void reload(Configuration config) {
    super.reload(config);
    this.connectTimeoutSeconds = config.getInt(CONNECT_TIMEOUT);
    this.readTimeoutSeconds = config.getInt(READ_TIMEOUT);
    this.languages = config.getStringList(LANGUAGES);
  }
}

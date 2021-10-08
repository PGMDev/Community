package dev.pgm.community.translations;

import dev.pgm.community.feature.config.FeatureConfigImpl;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.configuration.Configuration;

public class TranslationConfig extends FeatureConfigImpl {

  private static final String KEY = "translations";
  private static final String CONNECT_TIMEOUT = KEY + ".connect-timeout";
  private static final String READ_TIMEOUT = KEY + ".read-timeout";
  private static final String LANGUAGES = KEY + ".languages";
  private static final String API_KEY = KEY + ".api-key";

  private int connectTimeoutSeconds;
  private int readTimeoutSeconds;
  private Set<String> languages;
  private String apiKey;

  public TranslationConfig(Configuration config) {
    super(KEY, config);
  }

  public int getConnectTimeout() {
    return connectTimeoutSeconds;
  }

  public int getReadTimeout() {
    return readTimeoutSeconds;
  }

  public Set<String> getLanguages() {
    return languages;
  }

  public String getAPIKey() {
    return apiKey;
  }

  @Override
  public void reload(Configuration config) {
    super.reload(config);
    this.connectTimeoutSeconds = config.getInt(CONNECT_TIMEOUT);
    this.readTimeoutSeconds = config.getInt(READ_TIMEOUT);
    this.languages = config.getStringList(LANGUAGES).stream().collect(Collectors.toSet());
    this.apiKey = config.getString(API_KEY);
  }
}

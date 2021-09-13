package dev.pgm.community.translations;

import dev.pgm.community.feature.config.FeatureConfigImpl;
import java.util.List;
import org.bukkit.configuration.Configuration;

public class TranslationConfig extends FeatureConfigImpl {

  private static final String KEY = "translations";
  private static final String TIMEOUT = KEY + ".timeout";
  private static final String LANGUAGES = KEY + ".languages";

  private int timeoutSeconds;
  private List<String> languages;

  public TranslationConfig(Configuration config) {
    super(KEY, config);
  }

  public int getTimeout() {
    return timeoutSeconds;
  }

  public List<String> getLanguages() {
    return languages;
  }

  @Override
  public void reload(Configuration config) {
    super.reload(config);
    this.timeoutSeconds = config.getInt(TIMEOUT);
    this.languages = config.getStringList(LANGUAGES);
  }
}

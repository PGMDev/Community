package dev.pgm.community.translations;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Sets;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.feature.FeatureBase;
import dev.pgm.community.utils.PGMUtils;
import dev.pgm.community.utils.WebUtils;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import tc.oc.pgm.util.translation.Translation;

public class TranslationFeature extends FeatureBase {

  private PGMTranslationIntegration integration;

  private Cache<String, Translation> cache;

  public TranslationFeature(Configuration config, Logger logger) {
    super(new TranslationConfig(config), logger, "Translations (PGM)");
    this.cache = CacheBuilder.newBuilder().maximumSize(100).build();

    if (getConfig().isEnabled() && PGMUtils.isPGMEnabled()) {
      enable();
      this.integration = new PGMTranslationIntegration(this);
    }
  }

  private TranslationConfig getTranslationConfig() {
    return (TranslationConfig) getConfig();
  }

  @Override
  public Set<CommunityCommand> getCommands() {
    return getTranslationConfig().isEnabled()
        ? Sets.newHashSet(new TranslationCommand())
        : Sets.newHashSet();
  }

  public Set<String> getOnlineLanguages() {
    return Bukkit.getOnlinePlayers().stream()
        .map(Translation::getPlayerLanguageCode)
        .collect(Collectors.toSet());
  }

  public Set<String> getAcceptedLanguages() {
    return getTranslationConfig().getLanguages();
  }

  public List<Player> getOnline(String language) {
    return Bukkit.getOnlinePlayers().stream()
        .filter(p -> Translation.getPlayerLanguageCode(p).equalsIgnoreCase(language))
        .collect(Collectors.toList());
  }

  public CompletableFuture<Translation> translate(
      Player sender, String message, Set<String> languages) {
    if (languages.size() < 2) {
      // If there under 2 languages, no need to translate the message
      return CompletableFuture.completedFuture(new Translation(message));
    }

    // Check if message has been recently translated
    Translation cachedTranslation = cache.getIfPresent(message);
    if (cachedTranslation != null) {
      if (languages.stream().anyMatch(s -> !cachedTranslation.isTranslated(s))) {
        // Remove already translated
        languages.removeIf(lang -> cachedTranslation.isTranslated(lang));
        return WebUtils.getTranslated(cachedTranslation, languages, getTranslationConfig());
      }
      return CompletableFuture.completedFuture(cachedTranslation);
    }

    // Translation call will cache result
    return WebUtils.getTranslated(new Translation(message), languages, getTranslationConfig())
        .thenApplyAsync(
            translation -> {
              cache.put(message, translation);
              return translation;
            });
  }
}

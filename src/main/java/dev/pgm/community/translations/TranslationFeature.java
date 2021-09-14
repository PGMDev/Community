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
import tc.oc.pgm.api.integration.Integration;
import tc.oc.pgm.util.translation.Translation;

public class TranslationFeature extends FeatureBase {

  private PGMTranslationIntegration integration;

  public TranslationFeature(Configuration config, Logger logger) {
    super(new TranslationConfig(config), logger, "Translations (PGM)");

    if (getConfig().isEnabled()) {
      enable();
    }
  }

  private TranslationConfig getTranslationConfig() {
    return (TranslationConfig) getConfig();
  }

  @Override
  public void enable() {
    super.enable();
    integrate();
  }

  @Override
  public Set<CommunityCommand> getCommands() {
    return Sets.newHashSet(new TranslationCommand());
  }

  public Set<String> getOnlineLanguages() {
    return Bukkit.getOnlinePlayers().stream()
        .map(Translation::getPlayerLanguageCode)
        .collect(Collectors.toSet());
  }

  public List<Player> getOnline(String language) {
    return Bukkit.getOnlinePlayers().stream()
        .filter(p -> Translation.getPlayerLanguageCode(p).equalsIgnoreCase(language))
        .collect(Collectors.toList());
  }

  public CompletableFuture<Translation> translate(Player sender, String message) {
    Translation translation = new Translation(sender, message);
    return WebUtils.getTranslated(
        translation,
        getTranslationConfig().getLanguages(),
        getTranslationConfig().getConnectTimeout(),
        getTranslationConfig().getReadTimeout());
  }

  private void integrate() {
    if (PGMUtils.isPGMEnabled()) {
      this.integration = new PGMTranslationIntegration(this);
      Integration.setTranslationIntegration(integration);
    }
  }
}

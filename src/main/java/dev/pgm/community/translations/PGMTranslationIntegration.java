package dev.pgm.community.translations;

import java.util.concurrent.CompletableFuture;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.integration.TranslationIntegration;
import tc.oc.pgm.util.translation.Translation;

public class PGMTranslationIntegration implements TranslationIntegration {

  private TranslationFeature translation;

  public PGMTranslationIntegration(TranslationFeature translation) {
    this.translation = translation;
  }

  @Override
  public CompletableFuture<Translation> translate(Player sender, String message) {
    return translation.translate(sender, message, translation.getOnlineLanguages());
  }
}

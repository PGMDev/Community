package dev.pgm.community.translations;

import java.util.concurrent.CompletableFuture;
import tc.oc.pgm.api.integration.Integration;
import tc.oc.pgm.api.integration.TranslationIntegration;
import tc.oc.pgm.util.translation.Translation;

public class PGMTranslationIntegration implements TranslationIntegration {

  private TranslationFeature translation;

  public PGMTranslationIntegration(TranslationFeature translation) {
    this.translation = translation;
    Integration.setTranslationIntegration(this);
  }

  @Override
  public CompletableFuture<Translation> translate(String message) {
    return translation.translate(message, translation.getOnlineLanguages());
  }
}

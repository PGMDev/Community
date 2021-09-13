package dev.pgm.community.translations;

import static net.kyori.adventure.text.Component.text;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Description;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.utils.CommandAudience;
import java.util.concurrent.ExecutionException;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import tc.oc.pgm.util.translation.Translation;

public class TranslationCommand extends CommunityCommand {

  @Dependency private TranslationFeature translate;

  @CommandAlias("translate")
  @Description("Translate the given chat message")
  @CommandPermission(CommunityPermissions.TRANSLATE)
  public void translate(CommandAudience audience, Player player, String text) {
    Translation translation;
    try {
      translation = translate.translate(player, text).get();
      audience.sendMessage(
          text("Original: ", NamedTextColor.YELLOW).append(text(text, NamedTextColor.WHITE)));
      translation
          .getTranslated()
          .forEach(
              (locale, message) -> {
                audience.sendMessage(
                    text(locale + ": ", NamedTextColor.YELLOW)
                        .append(text(message, NamedTextColor.WHITE)));
              });

    } catch (InterruptedException | ExecutionException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    // });
  }
}

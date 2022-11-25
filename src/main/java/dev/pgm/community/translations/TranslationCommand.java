package dev.pgm.community.translations;

import static net.kyori.adventure.text.Component.text;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Description;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.utils.CommandAudience;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.PlayerComponent;
import tc.oc.pgm.util.text.TextFormatter;

public class TranslationCommand extends CommunityCommand {

  @Dependency private TranslationFeature translate;

  @CommandAlias("translate")
  @Description("Translate the given chat message")
  @CommandPermission(CommunityPermissions.TRANSLATE)
  public void translate(CommandAudience audience, String text) {
    translate
        .translate(text, translate.getAcceptedLanguages())
        .thenAcceptAsync(
            translation -> {
              audience.sendMessage(
                  text()
                      .append(text("Original", NamedTextColor.GOLD))
                      .append(text(": ", NamedTextColor.GRAY))
                      .append(text(text, NamedTextColor.WHITE))
                      .build());
              translation
                  .getTranslated()
                  .forEach(
                      (locale, message) -> {
                        audience.sendMessage(
                            text()
                                .append(text(" - "))
                                .append(text(locale, NamedTextColor.YELLOW))
                                .append(text(": "))
                                .append(text(message, NamedTextColor.WHITE))
                                .hoverEvent(
                                    HoverEvent.showText(
                                        text(
                                            "Click to paste message in chat", NamedTextColor.GRAY)))
                                .clickEvent(ClickEvent.suggestCommand(message))
                                .color(NamedTextColor.GRAY)
                                .build());
                      });
            });
  }

  @CommandAlias("languages")
  @Description("View a list of online languages")
  @CommandPermission(CommunityPermissions.TRANSLATE)
  public void languages(CommandAudience audience, @Default("false") boolean minimal) {
    Set<String> languages = translate.getOnlineLanguages();
    audience.sendMessage(
        text()
            .append(text("Online Languages", NamedTextColor.DARK_GREEN, TextDecoration.BOLD))
            .append(text(": ("))
            .append(text(languages.size(), NamedTextColor.GREEN))
            .append(text(")"))
            .color(NamedTextColor.GRAY)
            .build());

    if (minimal) {
      audience.sendMessage(
          TextFormatter.list(
              languages.stream()
                  .map(code -> text(code, NamedTextColor.YELLOW))
                  .collect(Collectors.toList()),
              NamedTextColor.GRAY));
      return;
    }

    for (String language : languages) {
      List<Player> online = translate.getOnline(language);
      audience.sendMessage(
          text()
              .color(NamedTextColor.GRAY)
              .append(text(" - "))
              .append(text(language, NamedTextColor.YELLOW))
              .append(text(": ("))
              .append(text(online.size(), NamedTextColor.GOLD))
              .append(text(") "))
              .append(
                  TextFormatter.list(
                      online.stream()
                          .map(player -> PlayerComponent.player(player, NameStyle.FANCY))
                          .collect(Collectors.toList()),
                      NamedTextColor.GRAY))
              .build());
    }
  }
}

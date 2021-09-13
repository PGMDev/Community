package dev.pgm.community.translations;

import static net.kyori.adventure.text.Component.text;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Description;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.utils.CommandAudience;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public class TranslationCommand extends CommunityCommand {

  @Dependency private TranslationFeature translate;

  @CommandAlias("translate")
  @Description("Translate the given chat message")
  @CommandPermission(CommunityPermissions.TRANSLATE)
  public void translate(CommandAudience audience, Player player, String text) {
    translate
        .translate(player, text)
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
}

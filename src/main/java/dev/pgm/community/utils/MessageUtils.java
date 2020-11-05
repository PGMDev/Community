package dev.pgm.community.utils;

import com.google.common.collect.Lists;
import java.util.List;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextDecoration;
import net.md_5.bungee.api.ChatColor;
import tc.oc.pgm.util.LegacyFormatUtils;
import tc.oc.pgm.util.text.TextTranslations;

public class MessageUtils {

  public static final Component DENY = TextComponent.of("\u2715", TextColor.DARK_RED);
  public static final Component ACCEPT = TextComponent.of("\u2714", TextColor.GREEN);
  public static final Component WARNING = TextComponent.of("\u26a0", TextColor.YELLOW);

  public static final Component CONSOLE =
      TranslatableComponent.of("misc.console", TextColor.DARK_AQUA, TextDecoration.ITALIC);

  public static String formatKickScreenMessage(String headerTitle, List<Component> lines) {
    List<Component> message = Lists.newArrayList();

    Component header =
        TextComponent.of(LegacyFormatUtils.horizontalLineHeading(headerTitle, ChatColor.DARK_GRAY));

    Component footer =
        TextComponent.of(
            LegacyFormatUtils.horizontalLine(
                ChatColor.DARK_GRAY, LegacyFormatUtils.MAX_CHAT_WIDTH));

    message.add(header); // Header Line - FIRST
    lines.forEach(message::add); // Add messages
    message.add(footer); // Footer Line - LAST

    return TextTranslations.translateLegacy(
        TextComponent.join(TextComponent.of("\n" + ChatColor.RESET), message), null);
  }

  public static Component formatUnseen(String target) {
    return TextComponent.builder()
        .append(target, TextColor.DARK_AQUA)
        .append(" has never joined the server")
        .build();
    // TODO: translate
  }
}

package dev.pgm.community.utils;

import static net.kyori.adventure.text.Component.text;

import com.google.common.base.Strings;
import net.kyori.adventure.text.Component;
import tc.oc.pgm.util.LegacyFormatUtils;
import tc.oc.pgm.util.text.TextTranslations;

public class CenterUtils {

  public static Component centerComponent(Component component) {
    int textWidth =
        LegacyFormatUtils.pixelWidth(TextTranslations.translateLegacy(component.asComponent()));
    int spaceCount =
        Math.max(
            0,
            ((LegacyFormatUtils.MAX_CHAT_WIDTH - textWidth) / 2 + 1)
                / (LegacyFormatUtils.SPACE_PIXEL_WIDTH + 1));
    String line = Strings.repeat(" ", spaceCount);
    return text().append(text(line)).append(component).build();
  }
}

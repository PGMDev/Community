package dev.pgm.community.moderation.tools.buttons;

import static net.kyori.adventure.text.Component.translatable;

import com.google.common.collect.Lists;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import tc.oc.pgm.util.text.TextTranslations;

public abstract class TranslatableToolButton extends ToolButtonBase {

  private String nameKey;
  private String loreKey;
  private NamedTextColor nameColor;

  public TranslatableToolButton(
      Player viewer,
      String nameKey,
      NamedTextColor color,
      String loreKey,
      Material material,
      int amount) {
    super(viewer, "", Lists.newArrayList(), material, amount);
    this.nameKey = nameKey;
    this.loreKey = loreKey;
    this.nameColor = color;
  }

  public NamedTextColor getColor() {
    return nameColor;
  }

  public Component getNameComponent() {
    return translatable(nameKey, getColor(), TextDecoration.BOLD);
  }

  public Component getLoreComponent() {
    return translatable(loreKey, NamedTextColor.GRAY);
  }

  @Override
  public String getName() {
    return TextTranslations.translateLegacy(getNameComponent(), getViewer());
  }

  @Override
  public List<String> getLore() {
    return Lists.newArrayList(TextTranslations.translateLegacy(getLoreComponent(), getViewer()));
  }
}
